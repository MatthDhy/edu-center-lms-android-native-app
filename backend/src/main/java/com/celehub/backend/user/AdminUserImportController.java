package com.celehub.backend.user;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.user.UserImportService.ImportResult;

@RestController
@RequestMapping("/api/admin/users/import")
public class AdminUserImportController {
    private final UserImportService service;

    public AdminUserImportController(UserImportService service) {
        this.service = service;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users_template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.template());
    }

    /** multipart: file (.xlsx). Mật khẩu tạm được gửi qua email từng người như khi tạo tay. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportResult> importUsers(@RequestPart("file") MultipartFile file) {
        ImportResult r = service.importFile(file);
        return ApiResponse.success("Đã tạo " + r.created() + " tài khoản, " + r.failed().size() + " dòng lỗi", r);
    }
}
