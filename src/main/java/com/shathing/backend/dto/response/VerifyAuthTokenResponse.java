package com.shathing.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VerifyAuthTokenResponse {

    private String accessToken;
}
