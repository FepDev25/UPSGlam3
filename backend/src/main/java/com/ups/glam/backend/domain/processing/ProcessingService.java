package com.ups.glam.backend.domain.processing;

import com.ups.glam.backend.shared.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessingService {

    private final ProcessingRepository processingRepository;
    private final GpuMetricsRepository gpuMetricsRepository;

    public Mono<PagedResponse<ProcessingHistoryResponse>> getHistory(UUID userId, int page, int size) {
        int offset = page * size;
        return processingRepository.findByUserIdPaged(userId, size, offset)
            .map(this::toHistoryResponse)
            .collectList()
            .zipWith(processingRepository.countByUserId(userId))
            .map(tuple -> PagedResponse.<ProcessingHistoryResponse>builder()
                .data(tuple.getT1())
                .page(page)
                .size(size)
                .hasNext((long) (offset + tuple.getT1().size()) < tuple.getT2())
                .build());
    }

    public Mono<ProcessingDetailResponse> getDetail(UUID id, UUID userId) {
        return processingRepository.findByIdAndUserId(id, userId)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Procesamiento no encontrado")))
            .flatMap(history -> gpuMetricsRepository.findByProcessingId(history.getId())
                .map(metrics -> toDetailResponse(history, metrics))
                .defaultIfEmpty(toDetailResponse(history, null))
            );
    }

    private ProcessingHistoryResponse toHistoryResponse(ProcessingHistory history) {
        return ProcessingHistoryResponse.builder()
            .id(history.getId())
            .filterId(history.getFilterId())
            .originalImageUrl(history.getOriginalImageUrl())
            .processedImageUrl(history.getProcessedImageUrl())
            .status(history.getStatus())
            .createdAt(history.getCreatedAt())
            .build();
    }

    private ProcessingDetailResponse toDetailResponse(ProcessingHistory history, GpuMetrics metrics) {
        return ProcessingDetailResponse.builder()
            .id(history.getId())
            .filterId(history.getFilterId())
            .originalImageUrl(history.getOriginalImageUrl())
            .processedImageUrl(history.getProcessedImageUrl())
            .status(history.getStatus())
            .createdAt(history.getCreatedAt())
            .gpuMetrics(metrics != null ? toMetricsResponse(metrics) : null)
            .build();
    }

    private GpuMetricsResponse toMetricsResponse(GpuMetrics m) {
        return GpuMetricsResponse.builder()
            .filterName(m.getFilterName())
            .imageWidth(m.getImageWidth())
            .imageHeight(m.getImageHeight())
            .blockDimX(m.getBlockDimX())
            .blockDimY(m.getBlockDimY())
            .gridDimX(m.getGridDimX())
            .gridDimY(m.getGridDimY())
            .totalThreads(m.getTotalThreads())
            .kernelTimeMs(m.getKernelTimeMs())
            .totalTimeMs(m.getTotalTimeMs())
            .memoryTransferredMb(m.getMemoryTransferredMb())
            .cudaVersion(m.getCudaVersion())
            .build();
    }
}
