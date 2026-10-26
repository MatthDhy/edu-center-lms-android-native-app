package com.celehub.backend.storage;

import java.net.URI;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.celehub.backend.config.AppProperties;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "supabase")
public class SupabaseStorageService implements StorageService {
    private final RestClient http;
    private final String objectBase;   // .../storage/v1/object/<bucket>/
    private final String publicBase;   // .../storage/v1/object/public/<bucket>/

    public SupabaseStorageService(AppProperties props) {
        var s = props.supabase();
        String root = s.url().replaceAll("/+$", "") + "/storage/v1/object/";
        this.objectBase = root + s.bucket() + "/";
        this.publicBase = root + "public/" + s.bucket() + "/";
        this.http = RestClient.builder()
                .defaultHeader("Authorization", "Bearer " + s.serviceKey())
                .defaultHeader("apikey", s.serviceKey())
                .build();
    }

    @Override
    public String upload(MultipartFile file, String folder) {
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String safeExt = (ext != null && ext.matches("[A-Za-z0-9]{1,8}")) ? "." + ext.toLowerCase() : "";
        String path = folder + "/" + UUID.randomUUID() + safeExt;   // folder do code truyền, không từ client
        try {
            String type = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            http.post().uri(URI.create(objectBase + path))
                .contentType(MediaType.parseMediaType(type))
                .body(file.getBytes())
                .retrieve().toBodilessEntity();
            return publicBase + path;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu tệp");
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(publicBase)) return;
        try {
            http.delete().uri(URI.create(objectBase + url.substring(publicBase.length())))
                .retrieve().toBodilessEntity();
        } catch (Exception ignored) { }
    }
}