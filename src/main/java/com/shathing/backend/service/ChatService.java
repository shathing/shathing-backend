package com.shathing.backend.service;

import com.shathing.backend.dto.request.SendChatMessageRequest;
import com.shathing.backend.dto.response.ChatMessageResponse;
import com.shathing.backend.dto.response.ChatMessageSliceResponse;
import com.shathing.backend.dto.response.ChatRoomResponse;
import com.shathing.backend.entity.ChatMessage;
import com.shathing.backend.entity.ChatRoom;
import com.shathing.backend.entity.Member;
import com.shathing.backend.repository.ChatMessageRepository;
import com.shathing.backend.repository.ChatRoomRepository;
import com.shathing.backend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ChatRoomResponse createOrGetDirectRoom(Long memberId, Long otherMemberId) {
        if (memberId.equals(otherMemberId)) {
            throw new IllegalArgumentException("자기 자신과는 채팅방을 만들 수 없습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        Member otherMember = memberRepository.findById(otherMemberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        String roomKey = buildRoomKey(memberId, otherMemberId);
        ChatRoom chatRoom = chatRoomRepository.findByRoomKey(roomKey)
                .orElseGet(() -> chatRoomRepository.save(createChatRoom(roomKey, member, otherMember)));

        return toChatRoomResponse(chatRoom, memberId);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getChatRooms(Long memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return chatRoomRepository.findAllByMemberIdOrderByRecentMessageDesc(memberId).stream()
                .map(chatRoom -> toChatRoomResponse(chatRoom, memberId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatMessageSliceResponse getChatMessages(Long memberId, Long roomId, Long beforeMessageId, int size) {
        ChatRoom chatRoom = getAccessibleRoom(memberId, roomId);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(0, normalizedSize + 1);

        List<ChatMessage> messages = beforeMessageId == null
                ? chatMessageRepository.findRecentMessagesByRoomId(chatRoom.getId(), pageable)
                : chatMessageRepository.findMessagesByRoomIdAndIdBefore(chatRoom.getId(), beforeMessageId, pageable);

        boolean hasNext = messages.size() > normalizedSize;
        if (hasNext) {
            messages = messages.subList(0, normalizedSize);
        }

        List<ChatMessageResponse> items = new ArrayList<>(messages.stream()
                .map(this::toChatMessageResponse)
                .toList());
        Collections.reverse(items);

        Long nextCursorId = hasNext && !items.isEmpty() ? items.get(0).getId() : null;
        return new ChatMessageSliceResponse(items, nextCursorId, hasNext);
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long memberId, Long roomId, SendChatMessageRequest request) {
        ChatRoom chatRoom = getAccessibleRoom(memberId, roomId);
        Member sender = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        ChatMessage chatMessage = chatMessageRepository.save(new ChatMessage(
                chatRoom,
                sender,
                request.getContent().trim()
        ));
        chatRoom.updateLastMessage(chatMessage.getContent(), chatMessage.getCreatedDate());

        return toChatMessageResponse(chatMessage);
    }

    private ChatRoom createChatRoom(String roomKey, Member member, Member otherMember) {
        if (member.getId() < otherMember.getId()) {
            return new ChatRoom(roomKey, member, otherMember);
        }
        return new ChatRoom(roomKey, otherMember, member);
    }

    private ChatRoom getAccessibleRoom(Long memberId, Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithMembers(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
        if (!chatRoom.isParticipant(memberId)) {
            throw new IllegalArgumentException("채팅방 참여자가 아닙니다.");
        }
        return chatRoom;
    }

    private ChatRoomResponse toChatRoomResponse(ChatRoom chatRoom, Long memberId) {
        Member otherMember = chatRoom.getOtherMember(memberId);
        return new ChatRoomResponse(
                chatRoom.getId(),
                new ChatRoomResponse.OtherMemberInfo(otherMember.getId(), otherMember.getUsername()),
                chatRoom.getLastMessage(),
                chatRoom.getLastMessageAt()
        );
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getRoom().getId(),
                new ChatMessageResponse.SenderInfo(
                        chatMessage.getSender().getId(),
                        chatMessage.getSender().getUsername()
                ),
                chatMessage.getContent(),
                chatMessage.getCreatedDate()
        );
    }

    private String buildRoomKey(Long memberId, Long otherMemberId) {
        long smaller = Math.min(memberId, otherMemberId);
        long larger = Math.max(memberId, otherMemberId);
        return smaller + ":" + larger;
    }
}
