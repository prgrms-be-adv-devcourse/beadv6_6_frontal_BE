package com.biddy.productservice.presentation.dto;

import java.util.List;
import java.util.UUID;

public record ProductIdsRequest(
        List<UUID> productIds
) {
}