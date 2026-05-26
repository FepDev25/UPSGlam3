package com.ups.glam.backend.infrastructure.cuda;

import lombok.Data;

@Data
class CudaMetrics {
    private String filterName;
    private Integer imageWidth;
    private Integer imageHeight;
    private Integer blockDimX;
    private Integer blockDimY;
    private Integer gridDimX;
    private Integer gridDimY;
    private Integer totalThreads;
    private Double kernelTimeMs;
    private Double totalTimeMs;
    private Double memoryTransferredMb;
    private Double gpuMemoryUsedMb;
    private String cudaVersion;
    private String status;
}
