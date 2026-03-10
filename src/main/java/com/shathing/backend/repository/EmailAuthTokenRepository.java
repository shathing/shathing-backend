package com.shathing.backend.repository;

import com.shathing.backend.entity.EmailAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailAuthTokenRepository extends JpaRepository<EmailAuthToken, Long> {

    Optional<EmailAuthToken> findByTokenHash(String tokenHash);

    Optional<EmailAuthToken> findByMember_Id(Long memberId);

    int deleteByExpiresAtBefore(LocalDateTime expiredBefore);
}
