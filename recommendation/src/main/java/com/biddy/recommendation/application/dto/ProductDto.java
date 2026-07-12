package com.biddy.recommendation.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String category,
        String brand,
        List<String> imageUrls
) {
}
