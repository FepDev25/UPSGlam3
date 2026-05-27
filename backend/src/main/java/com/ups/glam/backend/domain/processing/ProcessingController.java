package com.ups.glam.backend.domain.processing;

import com.ups.glam.backend.shared.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/processing")
@RequiredArgsConstructor
public class ProcessingController {

    private final ProcessingService processingService;

    @GetMapping("/history")
    public Mono<ResponseEntity<PagedResponse<ProcessingHistoryResponse>>> getHistory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return processingService.getHistory(userId, page, size)
            .map(ResponseEntity::ok);
    }

    @GetMapping("/history/{id}")
    public Mono<ResponseEntity<ProcessingDetailResponse>> getDetail(
        @PathVariable UUID id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return processingService.getDetail(id, userId)
            .map(ResponseEntity::ok);
    }
}
