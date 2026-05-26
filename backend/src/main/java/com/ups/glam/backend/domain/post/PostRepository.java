package com.ups.glam.backend.domain.post;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PostRepository extends ReactiveCrudRepository<Post, UUID> {

    @Query("SELECT * FROM posts WHERE is_published = true ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Post> findPublishedFeed(int limit, int offset);

    @Query("SELECT COUNT(*) FROM posts WHERE is_published = true")
    Mono<Long> countPublished();

    @Query("SELECT * FROM posts WHERE user_id = :userId AND is_published = true ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Post> findByUserIdPublished(UUID userId, int limit, int offset);

    @Query("SELECT COUNT(*) FROM posts WHERE user_id = :userId AND is_published = true")
    Mono<Long> countByUserId(UUID userId);

}
