package com.shathing.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePresignedUploadUrlRequest {

    @NotBlank
    private String fileName;

    @NotBlank
    private String contentType;
}
