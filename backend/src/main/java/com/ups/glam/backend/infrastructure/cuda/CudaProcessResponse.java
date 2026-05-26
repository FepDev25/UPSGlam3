package com.ups.glam.backend.infrastructure.cuda;

import lombok.Data;

@Data
class CudaProcessResponse {
    private String processedImageBase64;
    private CudaMetrics metrics;
}
