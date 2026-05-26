package com.ups.glam.backend.infrastructure.cuda;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
public class CudaServiceClient {

    private final WebClient webClient;

    public CudaServiceClient(@Qualifier("cudaWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<CudaResult> process(String imageUrl, String filterName, UUID processingId) {
        CudaProcessRequest request = CudaProcessRequest.builder()
            .imageUrl(imageUrl)
            .filterName(filterName)
            .processingId(processingId)
            .build();

        return webClient.post()
            .uri("/process")
            .bodyValue(request)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError(),
                response -> response.bodyToMono(String.class)
                    .doOnNext(body -> log.error("CUDA service 4xx: {}", body))
                    .flatMap(body -> Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Error en parámetros de procesamiento: " + body
                    )))
            )
            .onStatus(
                status -> status.is5xxServerError(),
                response -> response.bodyToMono(String.class)
                    .doOnNext(body -> log.error("CUDA service 5xx: {}", body))
                    .flatMap(body -> Mono.error(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "El servicio GPU no está disponible"
                    )))
            )
            .bodyToMono(CudaProcessResponse.class)
            .map(this::toCudaResult)
            .doOnSuccess(r -> log.debug("CUDA procesó imagen con filtro '{}' en {}ms",
                filterName, r.getTotalTimeMs()))
            .doOnError(e -> log.error("Error llamando al servicio CUDA: {}", e.getMessage()));
    }

    private CudaResult toCudaResult(CudaProcessResponse response) {
        CudaMetrics m = response.getMetrics();
        byte[] imageBytes = Base64.getDecoder().decode(response.getProcessedImageBase64());

        return CudaResult.builder()
            .processedImageBytes(imageBytes)
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
            .gpuMemoryUsedMb(m.getGpuMemoryUsedMb())
            .cudaVersion(m.getCudaVersion())
            .build();
    }
}
