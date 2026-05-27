package com.ups.glam.backend.domain.filter;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FilterResponse {
    private UUID id;
    private String name;
    private String displayName;
    private String description;
    private String iconName;
    private Boolean isActive;
}
