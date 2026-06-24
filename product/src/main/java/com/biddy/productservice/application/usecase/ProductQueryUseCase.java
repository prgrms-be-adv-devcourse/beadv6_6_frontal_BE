package com.biddy.productservice.application.usecase;

import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.model.SaleType;

import java.util.List;
import java.util.UUID;

public interface ProductQueryUseCase {

    Product getById(UUID id);

    List<Product> getAll();

    List<Product> getBySaleType(SaleType saleType);

}
