package com.ups.glam.backend.domain.processing;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProcessingRepository extends ReactiveCrudRepository<ProcessingHistory, UUID> {

    @Query("SELECT * FROM processing_history WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<ProcessingHistory> findByUserIdPaged(UUID userId, int limit, int offset);

    Mono<ProcessingHistory> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT COUNT(*) FROM processing_history WHERE user_id = :userId")
    Mono<Long> countByUserId(UUID userId);

    @Modifying
    @Query("UPDATE processing_history SET status = :status, processed_image_url = :processedImageUrl, post_id = :postId, updated_at = NOW() WHERE id = :id")
    Mono<Void> updateCompleted(UUID id, String status, String processedImageUrl, UUID postId);

    @Modifying
    @Query("UPDATE processing_history SET status = :status, error_message = :errorMessage, updated_at = NOW() WHERE id = :id")
    Mono<Void> updateFailed(UUID id, String status, String errorMessage);
}
