package com.biddy.productservice.application.usecase;

import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.model.SaleType;

import java.util.List;

public interface ProductQueryUseCase {

    Product getById(Long id);

    List<Product> getAll();

    List<Product> getBySaleType(SaleType saleType);

    List<Product> getProductsByIds(List<Long> ids);

}
