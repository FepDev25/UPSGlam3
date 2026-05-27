package com.ups.glam.backend.infrastructure.cuda;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CudaResult {
    private byte[] processedImageBytes;
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
}
