package com.shathing.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendAuthEmailRequest {
    @NotBlank
    @Email
    private String email;
}
