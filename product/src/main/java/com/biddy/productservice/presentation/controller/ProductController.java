package com.biddy.productservice.presentation.controller;


import com.biddy.productservice.application.usecase.ProductCommandUseCase;
import com.biddy.productservice.application.usecase.ProductQueryUseCase;
import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.model.SaleType;
import com.biddy.productservice.presentation.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.init}/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductCommandUseCase productCommandUseCase;
    private final ProductQueryUseCase productQueryUseCase;

    @PostMapping
    @Operation(summary = "상품 등록",description = "상품을 새로 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode="201",description="생성 성공",
            content = @Content(schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode="400",description="요청 오류")
    })
    public ResponseEntity<Product> create(@Valid @RequestBody ProductCreateRequest request){
        Product response = productCommandUseCase.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "상품 수정",description = "상품 정보를 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode="200",description="수정 성공",
                    content = @Content(schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode="404",description="상품 없음")
    })
    public Product update(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request){
        return productCommandUseCase.update(id,request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "상품 삭제", description = "상품을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "상품 없음")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id){
        productCommandUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "상품 목록 조회",description = "전체 또는 판매유형별 상품 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "조회 성공")
    })
    public List<Product> getProducts(
            @RequestParam(required = false) SaleType saleType){
        if (saleType != null) {
            return productQueryUseCase.getBySaleType(saleType);
        }
        return productQueryUseCase.getAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "상품 단일 조회",description = "Id로 상품 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "404", description = "상품 없음")
    })
    public Product getById(@PathVariable UUID id){
        return productQueryUseCase.getById(id);
    }


    @PostMapping("/details")
    public List<ProductInfoResponse> getProductsInfo(
            @RequestBody ProductIdsRequest request
    ) {
        return productQueryUseCase.getProductsByIds(request.productIds())
                .stream()
                .map(product -> new ProductInfoResponse(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getStatus(),
                        product.getSellerId(),
                        product.getStock()
                ))
                .toList();
    }

}
