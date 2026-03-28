package com.shathing.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class SharedItemResponse {

    private Long id;
    private String title;
    private String content;
    private List<String> photoUrls;
    private CategoryInfo category;
    private RegionInfo region;
    private MemberInfo member;
    private Instant createdDate;

    @Getter
    @AllArgsConstructor
    public static class CategoryInfo {
        private Long id;
        private String name;
    }

    @Getter
    @AllArgsConstructor
    public static class RegionInfo {
        private Long id;
        private String countryCode;
        private int depth;
        private String name;
        private String fullName;
    }

    @Getter
    @AllArgsConstructor
    public static class MemberInfo {
        private Long id;
        private String username;
    }
}
