package com.shathing.backend.service;

import com.shathing.backend.dto.request.SendAuthEmailRequest;
import com.shathing.backend.entity.Member;
import com.shathing.backend.entity.MemberStatus;
import com.shathing.backend.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void createMember() {
        // given
        String email = "yp071704@naver.com";
        SendAuthEmailRequest request = new SendAuthEmailRequest();
        request.setEmail(email);

        // when
        memberService.sendAuthEmail(request);

        // then
        Member member = memberRepository.findByEmail(email).orElseThrow();
        assertThat(member.getEmail()).isEqualTo(email);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.PENDING);
        assertThat(member.getUsername()).isEqualTo("yp071704");
    }
}
