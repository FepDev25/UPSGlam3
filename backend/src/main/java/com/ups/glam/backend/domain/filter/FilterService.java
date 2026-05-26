package com.ups.glam.backend.domain.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class FilterService {

    private final FilterRepository filterRepository;

    public Flux<FilterResponse> getActiveFilters() {
        return filterRepository.findByIsActiveTrueOrderByNameAsc()
            .map(this::toResponse);
    }

    private FilterResponse toResponse(Filter filter) {
        return FilterResponse.builder()
            .id(filter.getId())
            .name(filter.getName())
            .displayName(filter.getDisplayName())
            .description(filter.getDescription())
            .iconName(filter.getIconName())
            .isActive(filter.getIsActive())
            .build();
    }
}
