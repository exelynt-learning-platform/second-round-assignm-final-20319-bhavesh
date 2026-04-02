package com.shopease.service.impl;

import com.shopease.dto.request.ProductRequest;
import com.shopease.dto.response.PagedResponse;
import com.shopease.dto.response.ProductResponse;
import com.shopease.entity.Product;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.ProductMapper;
import com.shopease.repository.ProductRepository;
import com.shopease.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        product.setActive(true);
        
        Product savedProduct = productRepository.save(product);
        logger.info("Product created: {} (ID: {})", savedProduct.getName(), savedProduct.getId());
        
        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findProductById(id);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findByActiveTrue(pageable);
        
        Page<ProductResponse> responsePage = productPage.map(productMapper::toResponse);
        return PagedResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProductsByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Product> productPage = productRepository.findByActiveTrueAndCategoryIgnoreCase(category, pageable);
        
        Page<ProductResponse> responsePage = productPage.map(productMapper::toResponse);
        return PagedResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.searchProducts(keyword, pageable);
        
        Page<ProductResponse> responsePage = productPage.map(productMapper::toResponse);
        return PagedResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, 
                                                                    int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("price").ascending());
        Page<Product> productPage = productRepository.findByPriceRange(minPrice, maxPrice, pageable);
        
        Page<ProductResponse> responsePage = productPage.map(productMapper::toResponse);
        return PagedResponse.from(responsePage);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProductById(id);
        productMapper.updateEntity(product, request);
        
        Product updatedProduct = productRepository.save(product);
        logger.info("Product updated: {} (ID: {})", updatedProduct.getName(), updatedProduct.getId());
        
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        productRepository.delete(product);
        logger.info("Product deleted: {} (ID: {})", product.getName(), id);
    }

    @Override
    @Transactional
    public void activateProduct(Long id) {
        Product product = findProductById(id);
        product.setActive(true);
        productRepository.save(product);
        logger.info("Product activated: {} (ID: {})", product.getName(), id);
    }

    @Override
    @Transactional
    public void deactivateProduct(Long id) {
        Product product = findProductById(id);
        product.setActive(false);
        productRepository.save(product);
        logger.info("Product deactivated: {} (ID: {})", product.getName(), id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLatestProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findLatestProducts(pageable).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }
}
