package com.shopease.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopease.dto.request.ProductRequest;
import com.shopease.entity.Product;
import com.shopease.entity.Role;
import com.shopease.entity.User;
import com.shopease.repository.ProductRepository;
import com.shopease.repository.UserRepository;
import com.shopease.security.JwtTokenProvider;
import com.shopease.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String adminToken;
    private String userToken;
    private Product testProduct;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        User adminUser = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(adminUser);
        adminToken = "Bearer " + tokenProvider.generateToken(UserPrincipal.create(adminUser));

        User normalUser = User.builder()
                .firstName("Normal")
                .lastName("User")
                .email("user@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.USER)
                .enabled(true)
                .build();
        userRepository.save(normalUser);
        userToken = "Bearer " + tokenProvider.generateToken(UserPrincipal.create(normalUser));

        testProduct = Product.builder()
                .name("Test Product")
                .description("A test product description")
                .price(new BigDecimal("99.99"))
                .stockQuantity(50)
                .category("Electronics")
                .active(true)
                .build();
        testProduct = productRepository.save(testProduct);

        productRequest = ProductRequest.builder()
                .name("New Product")
                .description("A new product")
                .price(new BigDecimal("149.99"))
                .stockQuantity(100)
                .category("Electronics")
                .build();
    }

    @Test
    @DisplayName("Should get all products without authentication")
    void shouldGetAllProductsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Should get product by ID")
    void shouldGetProductById() throws Exception {
        mockMvc.perform(get("/api/products/" + testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Product"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent product")
    void shouldReturn404ForNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/products/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Admin should create product successfully")
    void adminShouldCreateProductSuccessfully() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Product"));
    }

    @Test
    @DisplayName("Regular user should not create product")
    void regularUserShouldNotCreateProduct() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should fail create product with invalid data")
    void shouldFailCreateProductWithInvalidData() throws Exception {
        productRequest.setName("");
        productRequest.setPrice(new BigDecimal("-10"));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @DisplayName("Admin should update product")
    void adminShouldUpdateProduct() throws Exception {
        productRequest.setName("Updated Product");

        mockMvc.perform(put("/api/products/" + testProduct.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Product"));
    }

    @Test
    @DisplayName("Admin should delete product")
    void adminShouldDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/products/" + testProduct.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should search products by keyword")
    void shouldSearchProductsByKeyword() throws Exception {
        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
