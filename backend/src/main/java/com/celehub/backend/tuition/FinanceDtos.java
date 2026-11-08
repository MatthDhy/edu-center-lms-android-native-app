package com.celehub.backend.tuition;

import java.util.List;

public final class FinanceDtos {
    private FinanceDtos() {}

    public record MonthRevenue(String month, long amount) {}

    /** Số liệu tài chính từng lớp (cộng dồn mọi khoản phải thu của lớp). */
    public record CourseFinance(Long courseId, String courseName, String teacherName, long studentCount,
                                long billed, long paid, long unpaid, long overdue,
                                long paidStudents, long debtStudents, long revenueInMonth, double collectionRate) {}

    public record Overview(String month, long revenueMonth, long revenueMonthVietQr, long revenueMonthCash,
                           long outstandingTotal, long outstandingCount, long overdueTotal, long overdueCount,
                           List<MonthRevenue> monthly, List<CourseFinance> courses) {}

    /** status: PAID | DEBT | OVERDUE | NO_BILL */
    public record StudentFinance(Long studentId, String fullName, String email, String phone,
                                 long billed, long paid, long debt, int bills, int overdueBills, String status) {}

    public record CourseFinanceDetail(CourseFinance course, List<StudentFinance> students) {}
}
