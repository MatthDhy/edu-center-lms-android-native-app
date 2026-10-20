package com.celehub.backend.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Mọi nơi cần upload file chỉ phụ thuộc interface này.
 * Hiện dùng LocalStorageService; sau đổi sang Firebase/R2/S3 chỉ cần thêm 1 class implements.
 */
public interface StorageService {
    /** @param folder vd "submissions", "materials", "avatars" — trả về URL để lưu vào DB */
    String upload(MultipartFile file, String folder);

    void delete(String url);
}
