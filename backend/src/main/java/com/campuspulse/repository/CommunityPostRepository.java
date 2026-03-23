package com.campuspulse.repository;

import java.util.List;

import com.campuspulse.model.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    List<CommunityPost> findTop20ByOrderByCreatedAtDesc();
}
