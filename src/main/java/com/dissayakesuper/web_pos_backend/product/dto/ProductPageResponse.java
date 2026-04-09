package com.dissayakesuper.web_pos_backend.product.dto;

import java.util.List;

import com.dissayakesuper.web_pos_backend.product.entity.Product;

public record ProductPageResponse(
        List<Product> content,
        long totalElements,
        int totalPages,
        int page,
        int limit,
        boolean hasNext,
        boolean hasPrevious
) {}
