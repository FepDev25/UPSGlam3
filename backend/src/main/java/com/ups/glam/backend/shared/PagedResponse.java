package com.ups.glam.backend.shared;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PagedResponse<T> {
    private List<T> data;
    private int page;
    private int size;
    private boolean hasNext;
}
