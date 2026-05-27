package com.ups.glam.backend.domain.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("filters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Filter {

    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;

    @Column("display_name")
    private String displayName;

    @Column("description")
    private String description;

    @Column("icon_name")
    private String iconName;

    @Column("is_active")
    private Boolean isActive;

    @Column("created_at")
    private OffsetDateTime createdAt;
}
