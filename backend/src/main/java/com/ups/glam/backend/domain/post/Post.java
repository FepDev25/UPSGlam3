package com.ups.glam.backend.domain.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @Column("id")
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("filter_id")
    private UUID filterId;

    @Column("caption")
    private String caption;

    @Column("original_image_url")
    private String originalImageUrl;

    @Column("processed_image_url")
    private String processedImageUrl;

    @Column("likes_count")
    private Integer likesCount;

    @Column("comments_count")
    private Integer commentsCount;

    @Column("is_published")
    private Boolean isPublished;

    @Column("created_at")
    private OffsetDateTime createdAt;

    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
