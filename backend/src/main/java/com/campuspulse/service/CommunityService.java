package com.campuspulse.service;

import java.util.List;

import com.campuspulse.dto.CommunityPostRequest;
import com.campuspulse.dto.CommunityPostResponse;
import com.campuspulse.model.CommunityPost;
import com.campuspulse.model.User;
import com.campuspulse.repository.CommunityPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityService {

    private final CommunityPostRepository communityPostRepository;
    private final AuthService authService;

    public CommunityService(CommunityPostRepository communityPostRepository, AuthService authService) {
        this.communityPostRepository = communityPostRepository;
        this.authService = authService;
    }

    @Transactional
    public CommunityPostResponse createPost(CommunityPostRequest request) {
        User user = authService.authenticate(request.token());

        CommunityPost post = new CommunityPost();
        post.setUser(user);
        post.setContent(request.content().trim());
        communityPostRepository.save(post);

        return new CommunityPostResponse(post.getId(), post.getContent(), post.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<CommunityPostResponse> latestPosts() {
        return communityPostRepository.findTop20ByOrderByCreatedAtDesc()
                .stream()
                .map(post -> new CommunityPostResponse(post.getId(), post.getContent(), post.getCreatedAt()))
                .toList();
    }
}
