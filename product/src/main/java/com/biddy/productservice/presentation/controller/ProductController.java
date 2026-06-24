package com.biddy.productservice.presentation.controller;


import com.biddy.productservice.application.service.ProductImageService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.init}/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductCommandUseCase productCommandUseCase;
    private final ProductQueryUseCase productQueryUseCase;
    private final ProductImageService productImageService;

    @PostMapping
    @Operation(summary = "상품 등록",description = "상품을 새로 등록합니다. 로그인 필요.")
    @ApiResponses({
            @ApiResponse(responseCode="201",description="생성 성공",
            content = @Content(schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode="400",description="요청 오류"),
            @ApiResponse(responseCode="401",description="인증 필요")
    })
    public ResponseEntity<Product> create(
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdStr,
            @Valid @RequestBody ProductCreateRequest request){
        if (memberIdStr == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long memberId = Long.parseLong(memberIdStr);
        Product response = productCommandUseCase.create(request, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "상품 수정",description = "상품 정보를 수정합니다. 로그인 필요.")
    @ApiResponses({
            @ApiResponse(responseCode="200",description="수정 성공",
                    content = @Content(schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode="404",description="상품 없음"),
            @ApiResponse(responseCode="401",description="인증 필요")
    })
    public ResponseEntity<Product> update(
            @RequestHeader(value = "X-Member-Id", required = false) String memberIdStr,
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateRequest request){
        if (memberIdStr == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long memberId = Long.parseLong(memberIdStr);
        return ResponseEntity.ok(productCommandUseCase.update(id, request, memberId));
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


    @PostMapping("/{id}/images")
    @Operation(summary = "상품 이미지 업로드", description = "상품에 이미지를 등록합니다. 최대 5장")
    public ResponseEntity<List<String>> uploadImages(
            @PathVariable UUID id,
            @RequestParam("images") List<MultipartFile> images) throws IOException {
        List<String> urls = productImageService.uploadImages(id, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(urls);
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
