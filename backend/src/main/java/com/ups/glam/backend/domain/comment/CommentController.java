package com.ups.glam.backend.domain.comment;

import com.ups.glam.backend.shared.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public Mono<ResponseEntity<PagedResponse<CommentResponse>>> getByPostId(
        @PathVariable UUID postId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return commentService.getByPostId(postId, page, size)
            .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<CommentResponse>> create(
        @PathVariable UUID postId,
        @RequestBody @Valid CreateCommentRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return commentService.create(postId, userId, request)
            .map(comment -> ResponseEntity.status(HttpStatus.CREATED).body(comment));
    }

    @DeleteMapping("/{commentId}")
    public Mono<ResponseEntity<Void>> delete(
        @PathVariable UUID postId,
        @PathVariable UUID commentId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return commentService.delete(postId, commentId, userId)
            .thenReturn(ResponseEntity.noContent().<Void>build());
    }
}
