package com.ups.glam.backend.domain.post;

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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/posts")
    public Mono<ResponseEntity<PagedResponse<PostResponse>>> getFeed(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return postService.getFeed(page, size)
            .map(ResponseEntity::ok);
    }

    @GetMapping("/posts/{id}")
    public Mono<ResponseEntity<PostResponse>> getById(@PathVariable UUID id) {
        return postService.getById(id)
            .map(ResponseEntity::ok);
    }

    @GetMapping("/profiles/{username}/posts")
    public Mono<ResponseEntity<PagedResponse<PostResponse>>> getByUsername(
        @PathVariable String username,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return postService.getByUsername(username, page, size)
            .map(ResponseEntity::ok);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<PostResponse>> create(
        @RequestBody @Valid CreatePostRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return postService.create(userId, request)
            .map(post -> ResponseEntity.status(HttpStatus.CREATED).body(post));
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> delete(
        @PathVariable UUID id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return postService.delete(id, userId)
            .thenReturn(ResponseEntity.noContent().<Void>build());
    }
}
