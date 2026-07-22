package com.makershub.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OrderProgressRequest {

    @NotNull
    @Min(0)
    @Max(100)
    private Integer percentage;

    @NotBlank
    @Size(max = 100)
    private String stageName;

    @Size(max = 1000)
    private String notes;

    @Size(max = 500)
    private String photoUrl;
}
