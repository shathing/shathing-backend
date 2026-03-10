package com.shathing.backend.scheduler;

import com.shathing.backend.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EmailAuthTokenCleanupScheduler {

    private final MemberService memberService;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanup() {
        memberService.cleanupOldEmailAuthTokens();
    }
}
