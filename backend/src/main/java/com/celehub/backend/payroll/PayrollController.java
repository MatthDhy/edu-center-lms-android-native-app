package com.celehub.backend.payroll;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.payroll.PayrollService.*;
import com.celehub.backend.security.AuthUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api")
public class PayrollController {
    public record RateRequest(@NotNull(message = "Nhập đơn giá") @Min(value = 0, message = "Đơn giá không hợp lệ") Long hourlyRate,
                              LocalDate effectiveFrom) {}

    private final PayrollService service;

    public PayrollController(PayrollService service) {
        this.service = service;
    }

    // ---- Admin ----
    @GetMapping("/admin/payroll/rates")
    public ApiResponse<List<RateDto>> rates() {
        return ApiResponse.success(service.rates());
    }

    @PutMapping("/admin/teachers/{id}/rate")
    public ApiResponse<Void> setRate(@PathVariable Long id, @Valid @RequestBody RateRequest req) {
        service.setRate(id, req.hourlyRate(), req.effectiveFrom());
        return ApiResponse.success("Đã cập nhật đơn giá", null);
    }

    @PostMapping("/admin/payroll/generate")
    public ApiResponse<GenerateResult> generate(@RequestParam(required = false) String month) {
        GenerateResult r = service.generate(month);
        return ApiResponse.success("Đã tính lương " + r.items() + " buổi dạy", r);
    }

    @GetMapping("/admin/payroll")
    public ApiResponse<PeriodDto> view(@RequestParam(required = false) String month) {
        return ApiResponse.success(service.view(month));
    }

    @GetMapping("/admin/payroll/items")
    public ApiResponse<List<ItemDto>> items(@RequestParam(required = false) String month,
                                            @RequestParam(required = false) Long teacherId) {
        return ApiResponse.success(service.items(month, teacherId));
    }

    @PatchMapping("/admin/payroll/{periodId}/approve")
    public ApiResponse<Void> approve(@PathVariable Long periodId) {
        service.approve(periodId);
        return ApiResponse.success("Đã duyệt bảng lương", null);
    }

    // ---- Giảng viên ----
    @GetMapping("/payroll")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<MyPayrollDto> mine(@AuthenticationPrincipal AuthUser me, @RequestParam(required = false) String month) {
        return ApiResponse.success(service.mine(me.id(), month));
    }
}
