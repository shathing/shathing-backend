package com.shathing.backend.dto.response;

import com.shathing.backend.entity.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberResponse {

    private Long memberId;
    private String email;
}
