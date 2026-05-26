package com.ups.glam.backend.domain.post;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PostResponse {
    private UUID id;
    private UUID userId;
    private UUID filterId;
    private String caption;
    private String originalImageUrl;
    private String processedImageUrl;
    private Integer likesCount;
    private Integer commentsCount;
    private Boolean isPublished;
    private OffsetDateTime createdAt;
    private AuthorInfo author;
}
