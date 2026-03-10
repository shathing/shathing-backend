package com.shathing.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyAuthEmailRequest {

    @NotBlank
    private String token;
}
