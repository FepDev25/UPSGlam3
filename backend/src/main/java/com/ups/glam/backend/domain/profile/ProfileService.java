package com.ups.glam.backend.domain.profile;

import com.ups.glam.backend.domain.post.AuthorInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    public Mono<ProfileResponse> getByUsername(String username) {
        return profileRepository.findByUsername(username)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado")))
            .map(this::toResponse);
    }

    public Mono<ProfileResponse> getMe(UUID userId) {
        return profileRepository.findById(userId)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado")))
            .map(this::toResponse);
    }

    public Mono<ProfileResponse> updateMe(UUID userId, UpdateProfileRequest request) {
        return profileRepository.findById(userId)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado")))
            .flatMap(profile -> {
                if (request.getFullName() != null) profile.setFullName(request.getFullName());
                if (request.getBio() != null) profile.setBio(request.getBio());
                if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl());
                profile.setUpdatedAt(OffsetDateTime.now());
                return profileRepository.save(profile);
            })
            .map(this::toResponse);
    }

    public Mono<AuthorInfo> getAuthorInfo(UUID userId) {
        return profileRepository.findById(userId)
            .map(p -> AuthorInfo.builder()
                .username(p.getUsername())
                .avatarUrl(p.getAvatarUrl())
                .build())
            .defaultIfEmpty(AuthorInfo.builder().username("unknown").build());
    }

    ProfileResponse toResponse(Profile profile) {
        return ProfileResponse.builder()
            .id(profile.getId())
            .username(profile.getUsername())
            .fullName(profile.getFullName())
            .bio(profile.getBio())
            .avatarUrl(profile.getAvatarUrl())
            .createdAt(profile.getCreatedAt())
            .build();
    }
}
