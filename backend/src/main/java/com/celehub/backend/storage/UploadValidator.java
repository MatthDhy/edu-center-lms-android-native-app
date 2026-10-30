package com.celehub.backend.storage;

import java.util.Set;
import java.util.TreeSet;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.celehub.backend.common.AppException;

/** Kiểm tra tệp upload: đuôi file nằm trong danh sách cho phép + dung lượng. */
public final class UploadValidator {
    public static final Set<String> DOCUMENTS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip", "png", "jpg", "jpeg", "webp");
    public static final Set<String> SUBMISSIONS = Set.of(
            "pdf", "doc", "docx", "png", "jpg", "jpeg", "webp", "heic");

    private UploadValidator() {}

    public static void require(MultipartFile file, Set<String> allowed, long maxBytes, String what) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tệp " + what);
        }
        if (file.getSize() > maxBytes) {
            throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE, "Tệp " + what + " vượt quá " + (maxBytes / 1024 / 1024) + "MB");
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || !allowed.contains(ext.toLowerCase())) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Định dạng tệp không được hỗ trợ (cho phép: " + String.join(", ", new TreeSet<>(allowed)) + ")");
        }
    }
}
