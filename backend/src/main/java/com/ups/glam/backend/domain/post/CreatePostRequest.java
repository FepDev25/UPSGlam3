package com.ups.glam.backend.domain.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreatePostRequest {
    @NotNull
    private UUID filterId;
    private String caption;
    @NotBlank
    private String originalImageUrl;
}
