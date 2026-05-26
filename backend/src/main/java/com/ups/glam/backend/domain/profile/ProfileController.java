package com.ups.glam.backend.domain.profile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profiles/{username}")
    public Mono<ResponseEntity<ProfileResponse>> getByUsername(@PathVariable String username) {
        return profileService.getByUsername(username)
            .map(ResponseEntity::ok);
    }

    @PatchMapping("/profiles/me")
    public Mono<ResponseEntity<ProfileResponse>> updateMe(
        @RequestBody @Valid UpdateProfileRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return profileService.updateMe(userId, request)
            .map(ResponseEntity::ok);
    }

    @GetMapping("/auth/me")
    public Mono<ResponseEntity<ProfileResponse>> getMe(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return profileService.getMe(userId)
            .map(ResponseEntity::ok);
    }
}
