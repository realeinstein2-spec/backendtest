package com.makershub.controller;

import com.makershub.dto.response.FileResponse;
import com.makershub.exception.BusinessException;
import com.makershub.util.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private FileController fileController;

    @Test
    void uploadFile_success_whenValidImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        when(cloudinaryService.upload(any())).thenReturn("https://res.cloudinary.com/test/image/upload/v123456/test.jpg");

        ResponseEntity<FileResponse.UploadResponse> response = fileController.uploadFile(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUrl()).isEqualTo("https://res.cloudinary.com/test/image/upload/v123456/test.jpg");
    }

    @Test
    void uploadFile_throwsBusinessException_whenUnsupportedMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "dummy pdf content".getBytes()
        );

        assertThatThrownBy(() -> fileController.uploadFile(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unsupported image type");
    }

    @Test
    void uploadFile_throwsBusinessException_whenEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThatThrownBy(() -> fileController.uploadFile(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Uploaded file cannot be empty");
    }
}
