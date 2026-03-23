package com.campuspulse.controller;

import java.util.List;

import com.campuspulse.dto.CommunityPostRequest;
import com.campuspulse.dto.CommunityPostResponse;
import com.campuspulse.service.CommunityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community/posts")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @PostMapping
    public CommunityPostResponse createPost(@Valid @RequestBody CommunityPostRequest request) {
        return communityService.createPost(request);
    }

    @GetMapping
    public List<CommunityPostResponse> latestPosts() {
        return communityService.latestPosts();
    }
}
