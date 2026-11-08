package com.celehub.backend.tuition;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.tuition.FinanceDtos.*;
import com.celehub.backend.tuition.TuitionDtos.*;

import jakarta.validation.Valid;

/** /api/admin/** chỉ ROLE_ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminFinanceController {
    private final TuitionService tuitionService;
    private final FinanceQueryService financeService;
    private final TuitionReminderService reminderService;

    public AdminFinanceController(TuitionService tuitionService, FinanceQueryService financeService,
                                  TuitionReminderService reminderService) {
        this.tuitionService = tuitionService;
        this.financeService = financeService;
        this.reminderService = reminderService;
    }

    // ---- báo cáo ----
    /** Doanh thu tháng, 12 tháng gần nhất, tình hình đóng học phí từng lớp. month = yyyy-MM. */
    @GetMapping("/finance/overview")
    public ApiResponse<Overview> overview(@RequestParam(required = false) String month) {
        return ApiResponse.success(financeService.overview(month));
    }

    /** Từng học viên của 1 lớp: đã đóng / còn nợ. */
    @GetMapping("/finance/courses/{courseId}")
    public ApiResponse<CourseFinanceDetail> courseDetail(@PathVariable Long courseId, @RequestParam(required = false) String month) {
        return ApiResponse.success(financeService.courseDetail(courseId, month));
    }

    // ---- công nợ ----
    @GetMapping("/tuition/bills")
    public ApiResponse<List<BillDto>> bills(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) Long courseId,
                                            @RequestParam(required = false) String search) {
        return ApiResponse.success(financeService.bills(status, courseId == null ? 0L : courseId, search));
    }

    @PostMapping("/tuition/bills")
    public ApiResponse<CreateBillsResult> create(@Valid @RequestBody CreateBillsRequest req) {
        CreateBillsResult r = tuitionService.createBills(req);
        return ApiResponse.success("Đã tạo " + r.created() + " khoản học phí", r);
    }

    @PostMapping("/tuition/bills/{id}/confirm-cash")
    public ApiResponse<BillDto> confirmCash(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success("Đã xác nhận thu tiền mặt", tuitionService.confirmCash(me, id));
    }

    @DeleteMapping("/tuition/bills/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tuitionService.deleteBill(id);
        return ApiResponse.success("Đã xóa khoản học phí", null);
    }

    /** Giả lập webhook ngân hàng (demo khi chưa có SePay/Casso). */
    @PostMapping("/tuition/simulate-payment")
    public ApiResponse<Map<String, String>> simulate(@Valid @RequestBody SimulateRequest req) {
        return ApiResponse.success("Đã xử lý giao dịch giả lập", Map.of("result", tuitionService.simulate(req)));
    }

    /** Gửi ngay thông báo học phí tháng này (bình thường job tự chạy 08:00 ngày 1). */
    @PostMapping("/tuition/send-monthly-reminder")
    public ApiResponse<Map<String, Integer>> remind() {
        return ApiResponse.success("Đã gửi thông báo học phí", Map.of("sent", reminderService.sendMonthly()));
    }
}
