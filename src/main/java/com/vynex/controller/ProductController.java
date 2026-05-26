package com.vynex.controller;

import com.vynex.model.Order;
import com.vynex.model.Product;
import com.vynex.repository.ProductRepository;
import com.vynex.session.SessionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductRepository productRepository;
    private final SessionManager sessionManager;

    public ProductController(ProductRepository productRepository, SessionManager sessionManager) {
        this.productRepository = productRepository;
        this.sessionManager = sessionManager;
    }

    // ── POST /api/products — business user adds a product ────────────────────
    @PostMapping
    public ResponseEntity<Map<String, String>> addProduct(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Product product) {

        if (!isValidSession(authHeader)) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        if (product.getName() == null || product.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Product name is required"));
        }

        productRepository.saveProduct(product);
        return ResponseEntity.ok(Map.of("message", "Product saved successfully"));
    }

    // ── GET /api/products — anyone can view products ──────────────────────────
    @GetMapping
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok(productRepository.getAllProducts());
    }

    // ── POST /api/products/order — customer places an order ──────────────────
    @PostMapping("/order")
    public ResponseEntity<Map<String, String>> placeOrder(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody List<Map<String, Object>> cartItems) {

        if (!isValidSession(authHeader)) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        String token = authHeader.substring(7);
        SessionManager.SessionData session = sessionManager.getSession(token);
        String customerEmail = session.email();

        for (Map<String, Object> item : cartItems) {
            Order order = new Order();
            order.setProductId(Long.parseLong(item.get("productId").toString()));
            order.setProductName(item.get("productName").toString());
            order.setCustomerEmail(customerEmail);
            order.setQuantity(Integer.parseInt(item.get("quantity").toString()));
            order.setDeliveryAddress(item.get("address").toString());
            order.setTotalPrice(Double.parseDouble(item.get("totalPrice").toString()));
            productRepository.saveOrder(order);
        }

        return ResponseEntity.ok(Map.of("message", "Order placed successfully"));
    }

    // ── GET /api/products/stats — business analytics data ────────────────────
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (!isValidSession(authHeader)) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("summary",     productRepository.getBusinessSummary());
        response.put("products",    productRepository.getProductStats());
        response.put("chartData",   productRepository.getOrdersLast7Days());
        return ResponseEntity.ok(response);
    }

    private boolean isValidSession(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
        return sessionManager.isValid(authHeader.substring(7));
    }
}
