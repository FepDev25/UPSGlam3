package com.ups.glam.backend.domain.processing;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GpuMetricsRepository extends ReactiveCrudRepository<GpuMetrics, UUID> {

    Mono<GpuMetrics> findByProcessingId(UUID processingId);
}
