package com.ups.glam.backend.domain.processing;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GpuMetricsResponse {
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
    private String cudaVersion;
}
