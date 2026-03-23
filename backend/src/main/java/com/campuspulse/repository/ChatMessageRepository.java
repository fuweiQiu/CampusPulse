package com.campuspulse.repository;

import java.util.List;

import com.campuspulse.model.ChatMessage;
import com.campuspulse.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop60ByUserOrderByCreatedAtDesc(User user);
}
