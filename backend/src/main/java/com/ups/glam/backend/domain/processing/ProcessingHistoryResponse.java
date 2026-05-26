package com.ups.glam.backend.domain.processing;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ProcessingHistoryResponse {
    private UUID id;
    private UUID filterId;
    private String originalImageUrl;
    private String processedImageUrl;
    private String status;
    private OffsetDateTime createdAt;
}
