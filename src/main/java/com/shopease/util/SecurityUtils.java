package com.shopease.util;

import com.shopease.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityUtils {

    public Optional<UserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) principal);
        }
        
        return Optional.empty();
    }

    public Long getCurrentUserId() {
        return getCurrentUser()
                .map(UserPrincipal::getId)
                .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    public String getCurrentUserEmail() {
        return getCurrentUser()
                .map(UserPrincipal::getEmail)
                .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    public boolean isCurrentUser(Long userId) {
        return getCurrentUser()
                .map(user -> user.getId().equals(userId))
                .orElse(false);
    }

    public boolean isAdmin() {
        return getCurrentUser()
                .map(user -> user.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")))
                .orElse(false);
    }

    public boolean canAccessResource(Long resourceUserId) {
        return isAdmin() || isCurrentUser(resourceUserId);
    }
}
