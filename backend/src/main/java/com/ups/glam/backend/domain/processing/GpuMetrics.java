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

@Table("gpu_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpuMetrics {

    @Id
    @Column("id")
    private UUID id;

    @Column("processing_id")
    private UUID processingId;

    @Column("filter_name")
    private String filterName;

    @Column("image_width")
    private Integer imageWidth;

    @Column("image_height")
    private Integer imageHeight;

    @Column("block_dim_x")
    private Integer blockDimX;

    @Column("block_dim_y")
    private Integer blockDimY;

    @Column("grid_dim_x")
    private Integer gridDimX;

    @Column("grid_dim_y")
    private Integer gridDimY;

    @Column("total_threads")
    private Integer totalThreads;

    @Column("kernel_time_ms")
    private Double kernelTimeMs;

    @Column("total_time_ms")
    private Double totalTimeMs;

    @Column("memory_transferred_mb")
    private Double memoryTransferredMb;

    @Column("gpu_memory_used_mb")
    private Double gpuMemoryUsedMb;

    @Column("cuda_version")
    private String cudaVersion;

    @Column("compute_capability")
    private String computeCapability;

    @Column("status")
    private String status;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
