package com.biddy.productservice.application.service;

import com.biddy.productservice.domain.model.Product;
import com.biddy.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductRepository productRepository;

    @Value("${image.upload.path:/app/images}")
    private String uploadPath;

    @Value("${image.base.url:http://localhost:8082/images}")
    private String baseUrl;

    @Transactional
    public List<String> uploadImages(Long productId, List<MultipartFile> files) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        List<String> savedUrls = files.stream().map(file -> {
            try {
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path filePath = uploadDir.resolve(fileName);
                file.transferTo(filePath.toFile());
                return baseUrl + "/" + fileName;
            } catch (IOException e) {
                throw new RuntimeException("이미지 저장 실패: " + file.getOriginalFilename(), e);
            }
        }).toList();

        product.addImageUrls(savedUrls);
        productRepository.save(product);

        return savedUrls;
    }
}
