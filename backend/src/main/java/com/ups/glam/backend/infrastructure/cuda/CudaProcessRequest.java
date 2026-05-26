package com.ups.glam.backend.infrastructure.cuda;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
class CudaProcessRequest {
    private String imageUrl;
    private String filterName;
    private UUID processingId;
}
