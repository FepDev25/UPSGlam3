package com.ups.glam.backend.domain.processing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("processing_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingHistory {

    @Id
    @Column("id")
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("filter_id")
    private UUID filterId;

    @Column("post_id")
    private UUID postId;

    @Column("original_image_url")
    private String originalImageUrl;

    @Column("processed_image_url")
    private String processedImageUrl;

    @Column("status")
    private String status;

    @Column("error_message")
    private String errorMessage;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
