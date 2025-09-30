package com.example.order_service.controller;

import com.example.order_service.dto.ProductDTO;
import com.example.order_service.entity.Product;
import com.example.order_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final ProductRepository productRepository;

    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String category) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<Product> products;

        if (category != null && !category.isEmpty()) {
            products = productRepository.findByCategory(category, pageRequest);
        } else {
            products = productRepository.findAll(pageRequest);
        }

        List<ProductDTO> productDTOs = products.getContent().stream()
                .map(ProductDTO::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable Long id) {
        return productRepository.findById(id)
            .map(ProductDTO::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products/featured")
    public ResponseEntity<List<ProductDTO>> getFeaturedProducts() {
        PageRequest pageRequest = PageRequest.of(0, 8, Sort.by("createdAt").descending());
        List<ProductDTO> products = productRepository.findAll(pageRequest).getContent()
                .stream()
                .map(ProductDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = productRepository.findDistinctCategories();
        return ResponseEntity.ok(categories);
    }
}