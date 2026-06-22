package com.biddy.productservice.application.usecase;

import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.presentation.dto.ProductCreateRequest;
import com.biddy.productservice.presentation.dto.ProductUpdateRequest;

import java.util.UUID;

public interface ProductCommandUseCase {

    Product create(ProductCreateRequest request);
    Product update(UUID id, ProductUpdateRequest request);
    void delete(UUID id);
}
