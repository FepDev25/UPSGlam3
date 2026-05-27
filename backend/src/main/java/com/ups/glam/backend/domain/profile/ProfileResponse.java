package com.ups.glam.backend.domain.profile;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ProfileResponse {
    private UUID id;
    private String username;
    private String fullName;
    private String bio;
    private String avatarUrl;
    private OffsetDateTime createdAt;
}
