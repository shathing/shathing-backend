package com.shathing.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Member extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private  String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    public Member(String email, String username, MemberStatus status) {
        this.email = email;
        this.username = username;
        this.status = status;
    }

    public void activate() {
        this.status = MemberStatus.ACTIVE;
    }
}
