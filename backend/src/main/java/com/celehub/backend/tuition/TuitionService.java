package com.celehub.backend.tuition;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.celehub.backend.common.AppException;
import com.celehub.backend.course.Course;
import com.celehub.backend.course.CourseService;
import com.celehub.backend.course.EnrollmentRepository;
import com.celehub.backend.notification.NotificationService;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.tuition.TuitionDtos.*;
import com.celehub.backend.user.Role;
import com.celehub.backend.user.User;
import com.celehub.backend.user.UserRepository;
import com.celehub.backend.user.UserStatus;

@Service
public class TuitionService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    /** Nội dung CK có dạng HP{idHV}C{idLớp}T{4 ký tự}. Ngân hàng có thể thêm chữ phía trước/sau nên dùng regex để tìm. */
    private static final Pattern TOKEN = Pattern.compile("HP\\d+C\\d+T[A-Z0-9]{4}");
    private static final String REF = "TUITION";

    private final TuitionBillRepository billRepository;
    private final TuitionPaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final NotificationService notificationService;
    private final String bankId;
    private final String bankName;
    private final String accountNumber;
    private final String accountName;

    public TuitionService(TuitionBillRepository billRepository, TuitionPaymentRepository paymentRepository,
                          UserRepository userRepository, EnrollmentRepository enrollmentRepository,
                          CourseService courseService, NotificationService notificationService,
                          @Value("${app.bank.id:MB}") String bankId,
                          @Value("${app.bank.name:MB Bank}") String bankName,
                          @Value("${app.bank.account-number:0000000000}") String accountNumber,
                          @Value("${app.bank.account-name:TRUNG TAM CELEHUB}") String accountName) {
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseService = courseService;
        this.notificationService = notificationService;
        this.bankId = bankId;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
    }

    // ===================== ADMIN =====================

    @Transactional
    public CreateBillsResult createBills(CreateBillsRequest req) {
        Course c = courseService.load(req.courseId());
        long amount = req.amount() != null ? req.amount() : c.getTuitionFee().longValue();
        if (amount <= 0) throw new AppException(HttpStatus.BAD_REQUEST, "Số tiền phải lớn hơn 0 (lớp chưa đặt học phí thì nhập số tiền)");
        String title = req.title().trim();

        Collection<Long> ids = req.studentIds() == null || req.studentIds().isEmpty()
                ? enrollmentRepository.findActiveStudentIds(c.getId()) : new LinkedHashSet<>(req.studentIds());
        int created = 0;
        int skipped = 0;
        for (Long sid : ids) {
            User u = sid == null ? null : userRepository.findById(sid).orElse(null);
            if (u == null || u.getRole() != Role.STUDENT || u.getStatus() != UserStatus.ACTIVE
                    || billRepository.existsByStudentIdAndCourseIdAndTitle(sid, c.getId(), title)) {
                skipped++;
                continue;
            }
            TuitionBill b = billRepository.save(new TuitionBill(u, c, title, BigDecimal.valueOf(amount), req.dueDate(),
                    newTransferContent(sid, c.getId())));
            notificationService.create(sid, "Khoản học phí mới: " + title,
                    c.getCourseName() + " - " + money(amount) + (req.dueDate() == null ? "" : " - hạn " + req.dueDate()),
                    c.getId(), REF, b.getId());
            created++;
        }
        return new CreateBillsResult(created, skipped);
    }

    @Transactional
    public BillDto confirmCash(AuthUser admin, Long billId) {
        TuitionBill b = billRepository.findDetail(billId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khoản học phí"));
        if (b.getStatus() == TuitionStatus.PAID) throw new AppException(HttpStatus.CONFLICT, "Khoản này đã thanh toán");
        BigDecimal remaining = b.getAmount().subtract(paymentRepository.sumByBill(billId));
        TuitionPayment p = pay(b, remaining, PaymentMethod.CASH, null, userRepository.getReferenceById(admin.id()));
        return toDto(b, b.getAmount().longValue(), p);
    }

    @Transactional
    public void deleteBill(Long billId) {
        TuitionBill b = billRepository.findById(billId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khoản học phí"));
        if (paymentRepository.countByBillId(billId) > 0) {
            throw new AppException(HttpStatus.CONFLICT, "Khoản này đã có giao dịch, không thể xóa");
        }
        billRepository.delete(b);
    }

    /** Dùng khi chưa có SePay/Casso: admin giả lập một giao dịch ngân hàng. */
    @Transactional
    public String simulate(SimulateRequest req) {
        String txn = "SIM" + System.currentTimeMillis();
        BigDecimal amount = req.amount() == null ? null : BigDecimal.valueOf(req.amount());
        return processBank(txn, req.transferContent(), amount);
    }

    /**
     * Xử lý 1 giao dịch tiền vào (webhook SePay/Casso hoặc giả lập).
     * @return DUPLICATE (đã xử lý) | MATCHED | UNMATCHED (không khớp khoản nào)
     */
    @Transactional
    public String processBank(String txnId, String content, BigDecimal amount) {
        if (txnId != null && !txnId.isBlank() && paymentRepository.findByBankTxnId(txnId).isPresent()) return "DUPLICATE";
        if (content == null || amount == null || amount.signum() <= 0) return "UNMATCHED";
        Matcher m = TOKEN.matcher(content.toUpperCase());
        while (m.find()) {
            TuitionBill b = billRepository.findByTransferContentDetail(m.group()).orElse(null);
            if (b == null || b.getStatus() == TuitionStatus.PAID) continue;
            pay(b, amount, PaymentMethod.VIETQR, txnId, null);
            return "MATCHED";
        }
        return "UNMATCHED";
    }

    private TuitionPayment pay(TuitionBill b, BigDecimal amount, PaymentMethod method, String txnId, User confirmedBy) {
        TuitionPayment p = paymentRepository.save(new TuitionPayment(b, amount, method, txnId, confirmedBy));
        BigDecimal total = paymentRepository.sumByBill(b.getId());
        if (total.compareTo(b.getAmount()) >= 0) {
            b.markPaid();
            notificationService.create(b.getStudent().getId(), "Đã nhận học phí",
                    b.getTitle() + " - " + b.getCourse().getCourseName() + ": " + money(b.getAmount().longValue()),
                    b.getCourse().getId(), REF, b.getId());
        } else {
            notificationService.create(b.getStudent().getId(), "Đã nhận một phần học phí",
                    b.getTitle() + ": nhận " + money(amount.longValue()) + ", còn thiếu "
                            + money(b.getAmount().subtract(total).longValue()),
                    b.getCourse().getId(), REF, b.getId());
        }
        return p;
    }

    // ===================== HỌC VIÊN =====================

    @Transactional(readOnly = true)
    public List<BillDto> myBills(AuthUser me) {
        List<TuitionBill> bills = billRepository.findByStudent(me.id());
        if (bills.isEmpty()) return List.of();
        Map<Long, List<TuitionPayment>> byBill = new HashMap<>();
        for (TuitionPayment p : paymentRepository.findByBillIds(bills.stream().map(TuitionBill::getId).toList())) {
            byBill.computeIfAbsent(p.getBill().getId(), k -> new ArrayList<>()).add(p);
        }
        List<BillDto> out = new ArrayList<>();
        for (TuitionBill b : bills) {
            List<TuitionPayment> ps = byBill.getOrDefault(b.getId(), List.of());
            long paid = ps.stream().mapToLong(x -> x.getAmount().longValue()).sum();
            out.add(toDto(b, paid, ps.isEmpty() ? null : ps.get(0)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public VietQrDto vietQr(AuthUser me, Long billId) {
        TuitionBill b = billRepository.findDetail(billId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy khoản học phí"));
        if (!b.getStudent().getId().equals(me.id())) throw new AppException(HttpStatus.FORBIDDEN, "Đây không phải khoản học phí của bạn");
        if (b.getStatus() == TuitionStatus.PAID) throw new AppException(HttpStatus.CONFLICT, "Khoản này đã thanh toán");
        long remaining = b.getAmount().subtract(paymentRepository.sumByBill(billId)).longValue();
        String url = "https://img.vietqr.io/image/" + bankId + "-" + accountNumber + "-compact2.png"
                + "?amount=" + remaining
                + "&addInfo=" + URLEncoder.encode(b.getTransferContent(), StandardCharsets.UTF_8)
                + "&accountName=" + URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        return new VietQrDto(b.getId(), b.getCourse().getCourseName(), b.getTitle(), remaining, bankName,
                accountNumber, accountName, b.getTransferContent(), url);
    }

    @Transactional(readOnly = true)
    public ReceiptDto receipt(AuthUser me, Long paymentId) {
        TuitionPayment p = paymentRepository.findDetail(paymentId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy biên lai"));
        TuitionBill b = p.getBill();
        if (me.role() == Role.STUDENT && !b.getStudent().getId().equals(me.id())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Đây không phải biên lai của bạn");
        }
        return new ReceiptDto(p.getId(), String.format("BL%06d", p.getId()), b.getId(), b.getStudent().getFullName(),
                b.getCourse().getCourseName(), b.getTitle(), p.getAmount().longValue(), p.getMethod().name(),
                p.getPaidAt(), b.getTransferContent());
    }

    // ===================== helpers =====================

    static String effective(TuitionBill b) {
        if (b.getStatus() == TuitionStatus.PAID) return "PAID";
        return b.getDueDate() != null && b.getDueDate().isBefore(LocalDate.now()) ? "OVERDUE" : "UNPAID";
    }

    private BillDto toDto(TuitionBill b, long paid, TuitionPayment last) {
        return new BillDto(b.getId(), b.getStudent().getId(), b.getStudent().getFullName(), b.getStudent().getEmail(),
                b.getCourse().getId(), b.getCourse().getCourseName(), b.getTitle(), b.getAmount().longValue(), paid,
                b.getDueDate(), effective(b), b.getTransferContent(), b.getCreatedAt(),
                last == null ? null : last.getPaidAt(), last == null ? null : last.getId());
    }

    private String newTransferContent(Long studentId, Long courseId) {
        for (int i = 0; i < 30; i++) {
            StringBuilder sb = new StringBuilder("HP").append(studentId).append('C').append(courseId).append('T');
            for (int k = 0; k < 4; k++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            if (!billRepository.existsByTransferContent(sb.toString())) return sb.toString();
        }
        throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được nội dung chuyển khoản, thử lại");
    }

    public static String money(long v) {
        return String.format("%,d", v).replace(',', '.') + " đ";
    }
}
