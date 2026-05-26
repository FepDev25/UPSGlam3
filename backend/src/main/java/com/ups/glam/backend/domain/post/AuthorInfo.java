package com.ups.glam.backend.domain.post;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorInfo {
    private String username;
    private String avatarUrl;
}
