package com.shathing.backend.repository;

import com.shathing.backend.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            select m
            from ChatMessage m
            join fetch m.sender
            where m.room.id = :roomId
            order by m.id desc
            """)
    List<ChatMessage> findRecentMessagesByRoomId(Long roomId, Pageable pageable);

    @Query("""
            select m
            from ChatMessage m
            join fetch m.sender
            where m.room.id = :roomId
              and m.id < :beforeMessageId
            order by m.id desc
            """)
    List<ChatMessage> findMessagesByRoomIdAndIdBefore(Long roomId, Long beforeMessageId, Pageable pageable);
}
