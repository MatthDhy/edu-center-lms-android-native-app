package com.celehub.backend.course;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.celehub.backend.common.ApiResponse;
import com.celehub.backend.course.dto.MaterialDto;
import com.celehub.backend.security.AuthUser;

@RestController
@RequestMapping("/api")
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) { this.materialService = materialService; }

    @GetMapping("/courses/{courseId}/materials")
    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    public ApiResponse<List<MaterialDto>> list(@AuthenticationPrincipal AuthUser me, @PathVariable Long courseId) {
        return ApiResponse.success(materialService.list(me, courseId));
    }

    /** multipart/form-data: title, description?, file */
    @PostMapping(value = "/courses/{courseId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<MaterialDto> upload(@AuthenticationPrincipal AuthUser me, @PathVariable Long courseId,
                                           @RequestParam String title,
                                           @RequestParam(required = false) String description,
                                           @RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Đã thêm tài liệu", materialService.upload(me, courseId, title, description, file));
    }

    @DeleteMapping("/materials/{id}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        materialService.delete(me, id);
        return ApiResponse.success("Đã xóa tài liệu", null);
    }
}