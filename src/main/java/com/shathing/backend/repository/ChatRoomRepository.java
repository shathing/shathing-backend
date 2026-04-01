package com.shathing.backend.repository;

import com.shathing.backend.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByRoomKey(String roomKey);

    @Query("""
            select r
            from ChatRoom r
            join fetch r.memberOne
            join fetch r.memberTwo
            where r.id = :roomId
            """)
    Optional<ChatRoom> findByIdWithMembers(Long roomId);

    @Query("""
            select r
            from ChatRoom r
            join fetch r.memberOne
            join fetch r.memberTwo
            where r.memberOne.id = :memberId
               or r.memberTwo.id = :memberId
            order by coalesce(r.lastMessageAt, r.createdDate) desc, r.id desc
            """)
    List<ChatRoom> findAllByMemberIdOrderByRecentMessageDesc(Long memberId);
}
