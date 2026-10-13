package com.devops.product.controller;

import com.devops.product.model.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/")
public class ProductController {

    private final List<Product> products = new ArrayList<>(Arrays.asList(
            new Product(1L, "Laptop Dell XPS 15", 1850.0, "Electronics", 25),
            new Product(2L, "Ban phim co Keychron K2", 85.0, "Accessories", 60),
            new Product(3L, "Chuot khong day Logitech MX Master 3S", 99.0, "Accessories", 40),
            new Product(4L, "Man hinh LG UltraFine 4K 27 inch", 450.0, "Displays", 15)
    ));

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRoot() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("message", "Product REST API - Deployed successfully via Docker Single-stage!");
        response.put("total_products", products.size());
        response.put("data", products);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(products);
    }

    @GetMapping("/api/products/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        return products.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
