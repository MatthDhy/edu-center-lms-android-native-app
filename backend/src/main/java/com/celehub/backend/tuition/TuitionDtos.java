package com.celehub.backend.tuition;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TuitionDtos {
    private TuitionDtos() {}

    /** studentIds rỗng/null = mọi học viên đang học lớp. amount null = học phí của lớp. Tiền = số nguyên VNĐ. */
    public record CreateBillsRequest(@NotNull(message = "Chọn lớp học") Long courseId,
                                     List<Long> studentIds,
                                     @NotBlank(message = "Tiêu đề không được để trống") @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự") String title,
                                     Long amount, LocalDate dueDate) {}

    public record CreateBillsResult(int created, int skipped) {}

    /** status hiệu lực: UNPAID | OVERDUE | PAID. */
    public record BillDto(Long id, Long studentId, String studentName, String studentEmail, Long courseId, String courseName,
                          String title, long amount, long paid, LocalDate dueDate, String status, String transferContent,
                          LocalDateTime createdAt, LocalDateTime paidAt, Long paymentId) {}

    public record VietQrDto(Long billId, String courseName, String title, long amount, String bankName, String accountNumber,
                            String accountName, String transferContent, String qrImageUrl) {}

    public record ReceiptDto(Long paymentId, String receiptNo, Long billId, String studentName, String courseName,
                             String title, long amount, String method, LocalDateTime paidAt, String transferContent) {}

    public record SimulateRequest(@NotBlank(message = "Thiếu nội dung chuyển khoản") String transferContent,
                                  Long amount) {}
}
