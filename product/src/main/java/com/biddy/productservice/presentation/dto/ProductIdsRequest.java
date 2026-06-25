package com.biddy.productservice.presentation.dto;

import java.util.List;

public record ProductIdsRequest(
        List<Long> productIds
) {
}