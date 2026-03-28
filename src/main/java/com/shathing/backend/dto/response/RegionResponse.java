package com.shathing.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegionResponse {

    private Long id;
    private String countryCode;
    private int depth;
    private String name;
    private String fullName;
}
