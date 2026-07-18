package com.makershub.controller;

import com.makershub.exception.BusinessException;
import com.makershub.util.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
@Tag(name = "File Uploads", description = "Upload images and documents to Cloudinary")
public class FileController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file to Cloudinary and get the secure URL")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Uploaded file cannot be empty", HttpStatus.BAD_REQUEST, "EMPTY_FILE");
        }
        try {
            String url = cloudinaryService.upload(file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalStateException ex) {
            throw new BusinessException("Cloudinary is not configured. Please set Cloudinary environment variables.", 
                    HttpStatus.BAD_REQUEST, "CLOUDINARY_NOT_CONFIGURED");
        } catch (IOException ex) {
            throw new BusinessException("Failed to upload file: " + ex.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR, "FILE_UPLOAD_ERROR");
        }
    }
}
