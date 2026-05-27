package com.ups.glam.backend.domain.like;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts/{postId}/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public Mono<ResponseEntity<Void>> like(
        @PathVariable UUID postId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return likeService.like(postId, userId)
            .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    @DeleteMapping
    public Mono<ResponseEntity<Void>> unlike(
        @PathVariable UUID postId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return likeService.unlike(postId, userId)
            .thenReturn(ResponseEntity.noContent().<Void>build());
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<LikedResponse>> hasLiked(
        @PathVariable UUID postId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return likeService.hasLiked(postId, userId)
            .map(ResponseEntity::ok);
    }
}
