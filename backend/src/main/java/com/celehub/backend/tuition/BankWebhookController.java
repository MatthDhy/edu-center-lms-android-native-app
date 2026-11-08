package com.celehub.backend.tuition;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.celehub.backend.common.AppException;

/**
 * Nhận webhook "tiền vào" từ SePay / Casso (khớp định dạng SePay):
 * header "Authorization: Apikey <BANK_WEBHOOK_SECRET>", body { id, content, transferAmount, transferType, referenceCode }.
 * Phải trả HTTP 2xx + {"success": true} thì SePay mới coi là đã nhận.
 */
@RestController
@RequestMapping("/api/webhooks")
public class BankWebhookController {
    private final TuitionService tuitionService;
    private final String secret;

    public BankWebhookController(TuitionService tuitionService, @Value("${app.bank.webhook-secret:}") String secret) {
        this.tuitionService = tuitionService;
        this.secret = secret;
    }

    @PostMapping("/bank")
    public Map<String, Object> bank(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @RequestHeader(value = "X-Webhook-Secret", required = false) String xSecret,
                                    @RequestBody Map<String, Object> body) {
        if (secret == null || secret.isBlank()) throw new AppException(HttpStatus.FORBIDDEN, "Webhook chưa được cấu hình");
        String given = xSecret != null ? xSecret : (auth == null ? "" : auth.replaceFirst("(?i)^(apikey|bearer)\\s+", ""));
        if (!MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8), given.trim().getBytes(StandardCharsets.UTF_8))) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Sai khóa webhook");
        }
        Object type = body.get("transferType");
        if (type != null && !"in".equalsIgnoreCase(String.valueOf(type))) return Map.of("success", true);   // bỏ qua tiền ra

        String txn = firstNonNull(body.get("id"), body.get("referenceCode"));
        String content = firstNonNull(body.get("content"), body.get("description"));
        BigDecimal amount = null;
        Object a = body.get("transferAmount") != null ? body.get("transferAmount") : body.get("amount");
        if (a != null) {
            try { amount = new BigDecimal(String.valueOf(a)); } catch (NumberFormatException ignored) { }
        }
        String result = tuitionService.processBank(txn, content, amount);
        return Map.of("success", true, "result", result);
    }

    private static String firstNonNull(Object a, Object b) {
        Object v = a != null ? a : b;
        return v == null ? null : String.valueOf(v);
    }
}
