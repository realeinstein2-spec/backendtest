package com.makershub.controller;

import com.makershub.dto.response.FileResponse;
import com.makershub.exception.BusinessException;
import com.makershub.util.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
@Tag(name = "File Uploads", description = "Upload images and documents to Cloudinary")
public class FileController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image file to Cloudinary and get the secure URL")
    public ResponseEntity<FileResponse.UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("Received file upload request: filename={}, contentType={}, size={} bytes",
                file.getOriginalFilename(), file.getContentType(), file.getSize());

        if (file.isEmpty()) {
            throw new BusinessException("Uploaded file cannot be empty", HttpStatus.BAD_REQUEST, "EMPTY_FILE");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            log.warn("Rejected file upload due to unsupported content-type: {}", contentType);
            throw new BusinessException("Unsupported image type", HttpStatus.BAD_REQUEST, "UNSUPPORTED_IMAGE_TYPE");
        }

        try {
            String url = cloudinaryService.upload(file);
            log.info("Successfully uploaded image to Cloudinary: url={}", url);
            return ResponseEntity.ok(FileResponse.UploadResponse.builder().url(url).build());
        } catch (IllegalStateException ex) {
            log.error("Cloudinary configuration error: {}", ex.getMessage());
            throw new BusinessException("Cloudinary is not configured. Please set Cloudinary environment variables.", 
                    HttpStatus.BAD_REQUEST, "CLOUDINARY_NOT_CONFIGURED");
        } catch (Exception ex) {
            log.error("Failed to upload file to Cloudinary", ex);
            throw new BusinessException("Failed to upload file: " + ex.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, "FILE_UPLOAD_ERROR");
        }
    }
}
