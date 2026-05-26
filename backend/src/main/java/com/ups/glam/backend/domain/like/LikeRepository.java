package com.ups.glam.backend.domain.like;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface LikeRepository extends ReactiveCrudRepository<Like, UUID> {

    Mono<Like> findByPostIdAndUserId(UUID postId, UUID userId);

    Mono<Boolean> existsByPostIdAndUserId(UUID postId, UUID userId);

    Mono<Void> deleteByPostIdAndUserId(UUID postId, UUID userId);
}
