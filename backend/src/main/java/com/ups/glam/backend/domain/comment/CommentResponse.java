package com.ups.glam.backend.domain.comment;

import com.ups.glam.backend.domain.post.AuthorInfo;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private UUID id;
    private UUID postId;
    private UUID userId;
    private String content;
    private OffsetDateTime createdAt;
    private AuthorInfo author;
}
