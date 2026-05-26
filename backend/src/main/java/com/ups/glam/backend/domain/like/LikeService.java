package com.ups.glam.backend.domain.like;

import com.ups.glam.backend.domain.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    @Transactional
    public Mono<Void> like(UUID postId, UUID userId) {
        return postRepository.existsById(postId)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Post no encontrado"));
                }
                return likeRepository.existsByPostIdAndUserId(postId, userId);
            })
            .flatMap(alreadyLiked -> {
                if (alreadyLiked) {
                    return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Ya diste like a este post"));
                }
                Like like = Like.builder()
                    .postId(postId)
                    .userId(userId)
                    .createdAt(OffsetDateTime.now())
                    .build();
                return likeRepository.save(like);
            })
            .then();
    }

    @Transactional
    public Mono<Void> unlike(UUID postId, UUID userId) {
        return likeRepository.existsByPostIdAndUserId(postId, userId)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No has dado like a este post"));
                }
                return likeRepository.deleteByPostIdAndUserId(postId, userId);
            })
            .then();
    }

    public Mono<LikedResponse> hasLiked(UUID postId, UUID userId) {
        return likeRepository.existsByPostIdAndUserId(postId, userId)
            .map(LikedResponse::new);
    }
}
