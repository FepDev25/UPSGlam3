package com.ups.glam.backend.domain.comment;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CommentRepository extends ReactiveCrudRepository<Comment, UUID> {

    @Query("SELECT * FROM comments WHERE post_id = :postId ORDER BY created_at ASC LIMIT :limit OFFSET :offset")
    Flux<Comment> findByPostIdPaged(UUID postId, int limit, int offset);

    @Query("SELECT COUNT(*) FROM comments WHERE post_id = :postId")
    Mono<Long> countByPostId(UUID postId);

    Mono<Comment> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("DELETE FROM comments WHERE id = :id AND user_id = :userId")
    Mono<Void> deleteByIdAndUserId(UUID id, UUID userId);
}
