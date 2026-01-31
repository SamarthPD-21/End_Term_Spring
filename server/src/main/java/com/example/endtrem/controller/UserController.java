package com.example.endtrem.controller;

import com.example.endtrem.dto.ApiResponse;
import com.example.endtrem.dto.UserDTO;
import com.example.endtrem.model.User;
import com.example.endtrem.repository.UserRepository;
import com.example.endtrem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(ApiResponse.success(mapToDTO(user)));
        } catch (Exception e) {
            log.error("Failed to get profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update current user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequest request) {
        try {
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (request.getName() != null && !request.getName().isBlank()) {
                user.setName(request.getName());
            }
            if (request.getAvatarUrl() != null) {
                user.setAvatarUrl(request.getAvatarUrl());
            }
            
            user = userRepository.save(user);
            log.info("Profile updated for user: {}", user.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", mapToDTO(user)));
        } catch (Exception e) {
            log.error("Failed to update profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get all users (admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        try {
            List<UserDTO> users = userRepository.findAll().stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            log.error("Failed to get all users: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get user by ID (admin only)
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable String userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(ApiResponse.success(mapToDTO(user)));
        } catch (Exception e) {
            log.error("Failed to get user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete current user account
     */
    @DeleteMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            userRepository.deleteById(principal.getId());
            log.info("Account deleted for user: {}", principal.getEmail());
            return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));
        } catch (Exception e) {
            log.error("Failed to delete account: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .githubUsername(user.getGithubUsername())
                .avatarUrl(user.getAvatarUrl())
                .hasGithubLinked(user.getGithubAccessToken() != null)
                .lastAnalysisAt(user.getLastAnalysisAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // Inner class for profile update request
    @lombok.Data
    public static class UpdateProfileRequest {
        private String name;
        private String avatarUrl;
    }
}
