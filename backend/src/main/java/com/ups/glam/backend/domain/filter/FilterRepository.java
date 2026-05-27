package com.ups.glam.backend.domain.filter;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface FilterRepository extends ReactiveCrudRepository<Filter, UUID> {

    Flux<Filter> findByIsActiveTrueOrderByNameAsc();
}
