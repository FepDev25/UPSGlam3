package com.ups.glam.backend.domain.profile;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ProfileRepository extends ReactiveCrudRepository<Profile, UUID> {

    Mono<Profile> findByUsername(String username);
}
