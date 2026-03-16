package com.shathing.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LegalDongResponse {

    private List<LegalDongItem> legalDongs;

    @Getter
    @AllArgsConstructor
    public static class LegalDongItem {
        private String code;
        private String name;
    }
}
