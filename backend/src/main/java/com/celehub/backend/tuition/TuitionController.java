package com.celehub.backend.tuition;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.security.AuthUser;
import com.celehub.backend.tuition.TuitionDtos.*;

/** Tab Học phí của học viên. */
@RestController
@RequestMapping("/api/tuition")
public class TuitionController {
    private final TuitionService service;

    public TuitionController(TuitionService service) {
        this.service = service;
    }

    @GetMapping("/bills")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<BillDto>> bills(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.success(service.myBills(me));
    }

    @GetMapping("/bills/{id}/vietqr")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<VietQrDto> vietQr(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return ApiResponse.success(service.vietQr(me, id));
    }

    @GetMapping("/receipts/{paymentId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ApiResponse<ReceiptDto> receipt(@AuthenticationPrincipal AuthUser me, @PathVariable Long paymentId) {
        return ApiResponse.success(service.receipt(me, paymentId));
    }
}
