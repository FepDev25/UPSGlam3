package com.ups.glam.backend.domain.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/filters")
@RequiredArgsConstructor
public class FilterController {

    private final FilterService filterService;

    @GetMapping
    public Mono<ResponseEntity<List<FilterResponse>>> getActiveFilters() {
        return filterService.getActiveFilters()
            .collectList()
            .map(ResponseEntity::ok);
    }
}
