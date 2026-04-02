package com.shopease.service;

import com.shopease.dto.request.ProductRequest;
import com.shopease.dto.response.PagedResponse;
import com.shopease.dto.response.ProductResponse;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse getProductById(Long id);

    PagedResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir);

    PagedResponse<ProductResponse> getProductsByCategory(String category, int page, int size);

    PagedResponse<ProductResponse> searchProducts(String keyword, int page, int size);

    PagedResponse<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, 
                                                            int page, int size);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    void activateProduct(Long id);

    void deactivateProduct(Long id);

    List<String> getAllCategories();

    List<ProductResponse> getLatestProducts(int limit);
}
