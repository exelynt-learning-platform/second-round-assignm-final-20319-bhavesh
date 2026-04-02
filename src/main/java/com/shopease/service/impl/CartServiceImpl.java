package com.shopease.service.impl;

import com.shopease.dto.request.CartItemRequest;
import com.shopease.dto.request.UpdateCartItemRequest;
import com.shopease.dto.response.CartResponse;
import com.shopease.entity.Cart;
import com.shopease.entity.CartItem;
import com.shopease.entity.Product;
import com.shopease.entity.User;
import com.shopease.exception.InsufficientStockException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.mapper.CartMapper;
import com.shopease.repository.CartRepository;
import com.shopease.repository.ProductRepository;
import com.shopease.repository.UserRepository;
import com.shopease.service.CartService;
import com.shopease.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final SecurityUtils securityUtils;

    public CartServiceImpl(CartRepository cartRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository,
                           CartMapper cartMapper,
                           SecurityUtils securityUtils) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cartMapper = cartMapper;
        this.securityUtils = securityUtils;
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        Cart cart = getOrCreateCart();
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(CartItemRequest request) {
        Cart cart = getOrCreateCart();
        Product product = findActiveProduct(request.getProductId());

        validateStock(product, request.getQuantity());

        Optional<CartItem> existingItem = cart.findItemByProductId(product.getId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            validateStock(product, newQuantity);
            item.setQuantity(newQuantity);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        logger.info("Product {} added to cart for user {}", product.getName(), securityUtils.getCurrentUserEmail());
        
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long productId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart();
        
        CartItem cartItem = cart.findItemByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "productId", productId));

        validateStock(cartItem.getProduct(), request.getQuantity());
        cartItem.setQuantity(request.getQuantity());

        Cart savedCart = cartRepository.save(cart);
        logger.info("Cart item updated for product {} in user {}'s cart", productId, securityUtils.getCurrentUserEmail());
        
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse removeFromCart(Long productId) {
        Cart cart = getOrCreateCart();
        
        boolean removed = cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        
        if (!removed) {
            throw new ResourceNotFoundException("Cart item", "productId", productId);
        }

        Cart savedCart = cartRepository.save(cart);
        logger.info("Product {} removed from cart for user {}", productId, securityUtils.getCurrentUserEmail());
        
        return cartMapper.toResponse(savedCart);
    }

    @Override
    @Transactional
    public void clearCart() {
        Cart cart = getOrCreateCart();
        cart.clear();
        cartRepository.save(cart);
        logger.info("Cart cleared for user {}", securityUtils.getCurrentUserEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount() {
        Cart cart = getOrCreateCart();
        return cart.getTotalItems();
    }

    private Cart getOrCreateCart() {
        Long userId = securityUtils.getCurrentUserId();
        
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private Product findActiveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        if (!product.isActive()) {
            throw new ResourceNotFoundException("Product is not available");
        }
        
        return product;
    }

    private void validateStock(Product product, int requestedQuantity) {
        if (!product.hasStock(requestedQuantity)) {
            throw new InsufficientStockException(
                    product.getName(),
                    requestedQuantity,
                    product.getStockQuantity()
            );
        }
    }
}
