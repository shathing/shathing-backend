package com.shathing.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateSharedItemRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotEmpty
    private List<@NotBlank String> photoUrls;

    @NotNull
    private Long categoryId;

    @NotNull
    private Long regionId;
}
