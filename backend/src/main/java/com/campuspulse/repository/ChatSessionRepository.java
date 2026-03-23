package com.campuspulse.repository;

import java.util.Optional;

import com.campuspulse.model.ChatSession;
import com.campuspulse.model.ChatSessionStatus;
import com.campuspulse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findFirstByUserAndStatusOrderByUpdatedAtDesc(User user, ChatSessionStatus status);
}
