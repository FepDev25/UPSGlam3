package com.ups.glam.backend.infrastructure.supabase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class SupabaseStorageClient {

    private static final String PROCESSED_BUCKET = "processed-images";

    private final WebClient webClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    public SupabaseStorageClient(@Qualifier("supabaseStorageWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> uploadProcessedImage(UUID userId, UUID processingId, byte[] imageBytes) {
        String path = userId + "/" + processingId + ".jpg";

        return webClient.post()
            .uri("/object/" + PROCESSED_BUCKET + "/" + path)
            .contentType(MediaType.IMAGE_JPEG)
            .bodyValue(imageBytes)
            .retrieve()
            .onStatus(
                status -> status.is4xxClientError(),
                response -> response.bodyToMono(String.class)
                    .doOnNext(body -> log.error("Supabase Storage 4xx: {}", body))
                    .flatMap(body -> Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Parámetros inválidos para subir imagen"
                    )))
            )
            .onStatus(
                status -> status.is5xxServerError(),
                response -> response.bodyToMono(String.class)
                    .doOnNext(body -> log.error("Supabase Storage 5xx: {}", body))
                    .flatMap(body -> Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Error al subir imagen a Supabase Storage"
                    )))
            )
            .toBodilessEntity()
            .thenReturn(buildPublicUrl(path))
            .doOnSuccess(url -> log.debug("Imagen subida a Storage: {}", url))
            .doOnError(e -> log.error("Error subiendo imagen a Storage: {}", e.getMessage()));
    }

    private String buildPublicUrl(String path) {
        return supabaseUrl + "/storage/v1/object/public/" + PROCESSED_BUCKET + "/" + path;
    }
}
