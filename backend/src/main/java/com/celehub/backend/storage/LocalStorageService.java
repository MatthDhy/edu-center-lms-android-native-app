package com.celehub.backend.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.celehub.backend.common.AppException;
import com.celehub.backend.config.AppProperties;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {
    private final Path root;
    private final String filesBaseUrl;

    public LocalStorageService(AppProperties props) throws IOException {
        this.root = Path.of(props.storage().localDir()).toAbsolutePath().normalize();
        this.filesBaseUrl = props.storage().publicBaseUrl() + "/files/";
        Files.createDirectories(root);
    }

    @Override
    public String upload(MultipartFile file, String folder) {
        try {
            Path dir = root.resolve(folder).normalize();
            if (!dir.startsWith(root)) throw new AppException(HttpStatus.BAD_REQUEST, "Thư mục không hợp lệ");
            Files.createDirectories(dir);

            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String safeExt = (ext != null && ext.matches("[A-Za-z0-9]{1,8}")) ? "." + ext.toLowerCase() : "";
            String name = UUID.randomUUID() + safeExt;

            file.transferTo(dir.resolve(name));
            return filesBaseUrl + folder + "/" + name;
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu tệp");
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(filesBaseUrl)) return;
        try {
            Path p = root.resolve(url.substring(filesBaseUrl.length())).normalize();
            if (p.startsWith(root)) Files.deleteIfExists(p);
        } catch (IOException ignored) { }
    }
}
