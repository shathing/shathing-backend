package com.shathing.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "chat_room",
        indexes = {
                @Index(name = "idx_chat_room_room_key", columnList = "room_key", unique = true),
                @Index(name = "idx_chat_room_last_message_at", columnList = "last_message_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_key", nullable = false, unique = true)
    private String roomKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_one_id", nullable = false)
    private Member memberOne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_two_id", nullable = false)
    private Member memberTwo;

    @Column(name = "last_message")
    private String lastMessage;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    public ChatRoom(String roomKey, Member memberOne, Member memberTwo) {
        this.roomKey = roomKey;
        this.memberOne = memberOne;
        this.memberTwo = memberTwo;
    }

    public boolean isParticipant(Long memberId) {
        return memberOne.getId().equals(memberId) || memberTwo.getId().equals(memberId);
    }

    public Member getOtherMember(Long memberId) {
        if (memberOne.getId().equals(memberId)) {
            return memberTwo;
        }
        if (memberTwo.getId().equals(memberId)) {
            return memberOne;
        }
        throw new IllegalArgumentException("채팅방 참여자가 아닙니다.");
    }

    public void updateLastMessage(String lastMessage, Instant lastMessageAt) {
        this.lastMessage = lastMessage;
        this.lastMessageAt = lastMessageAt;
    }
}
