package com.example.endtrem.service;

import com.example.endtrem.dto.*;
import com.example.endtrem.model.User;
import com.example.endtrem.repository.*;
import com.example.endtrem.security.JwtService;
import com.example.endtrem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    
    // Repositories for cascade delete
    private final AnalysisResultRepository analysisResultRepository;
    private final SkillProfileRepository skillProfileRepository;
    private final SkillProgressionRepository skillProgressionRepository;
    private final RoleInferenceRepository roleInferenceRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final RepositoryRepository repositoryRepository;
    private final RepoSummaryRepository repoSummaryRepository;
    
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .authProvider(User.AuthProvider.LOCAL)
                .build();
        
        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());
        
        return generateAuthResponse(user);
    }
    
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        log.info("User logged in: {}", user.getEmail());
        return generateAuthResponse(user);
    }
    
    public AuthResponse processGitHubUser(String githubId, String email, String name, 
                                          String username, String avatarUrl, String accessToken) {
        User user = userRepository.findByGithubId(githubId)
                .orElseGet(() -> {
                    // Check if user exists with same email
                    return userRepository.findByEmail(email)
                            .map(existingUser -> {
                                // Link GitHub to existing account
                                existingUser.setGithubId(githubId);
                                existingUser.setGithubUsername(username);
                                existingUser.setGithubAccessToken(accessToken);
                                existingUser.setAvatarUrl(avatarUrl);
                                return existingUser;
                            })
                            .orElseGet(() -> User.builder()
                                    .email(email)
                                    .name(name)
                                    .githubId(githubId)
                                    .githubUsername(username)
                                    .githubAccessToken(accessToken)
                                    .avatarUrl(avatarUrl)
                                    .authProvider(User.AuthProvider.GITHUB)
                                    .build());
                });
        
        // Update access token on each login
        user.setGithubAccessToken(accessToken);
        user = userRepository.save(user);
        
        log.info("GitHub user processed: {}", user.getGithubUsername());
        return generateAuthResponse(user);
    }
    
    /**
     * Link GitHub account to an existing logged-in user.
     * This allows users with different emails to link their GitHub.
     */
    public UserDTO linkGitHubToUser(String userId, String githubId, String username, 
                                     String avatarUrl, String accessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if this GitHub account is already linked to another user
        userRepository.findByGithubId(githubId).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(userId)) {
                throw new RuntimeException("This GitHub account is already linked to another user");
            }
        });
        
        user.setGithubId(githubId);
        user.setGithubUsername(username);
        user.setGithubAccessToken(accessToken);
        if (user.getAvatarUrl() == null) {
            user.setAvatarUrl(avatarUrl);
        }
        
        user = userRepository.save(user);
        log.info("GitHub linked to user: {} -> {}", user.getEmail(), username);
        
        return mapToUserDTO(user);
    }
    
    /**
     * Unlink GitHub account from a user.
     */
    public UserDTO unlinkGitHub(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Only allow unlinking if user has a password (local auth)
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new RuntimeException("Cannot unlink GitHub. Please set a password first.");
        }
        
        user.setGithubId(null);
        user.setGithubUsername(null);
        user.setGithubAccessToken(null);
        
        user = userRepository.save(user);
        log.info("GitHub unlinked from user: {}", user.getEmail());
        
        return mapToUserDTO(user);
    }
    
    private AuthResponse generateAuthResponse(User user) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String token = jwtService.generateToken(userPrincipal);
        String refreshToken = jwtService.generateRefreshToken(userPrincipal);
        
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(mapToUserDTO(user))
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }
    
    public UserDTO getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToUserDTO(user);
    }
    
    private UserDTO mapToUserDTO(User user) {
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
    
    /**
     * Delete user account and all associated data
     */
    @Transactional
    public void deleteAccount(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        log.info("Deleting account for user: {} ({})", user.getEmail(), userId);
        
        // Delete all user data from all collections
        analysisResultRepository.deleteByUserId(userId);
        skillProfileRepository.deleteByUserId(userId);
        skillProgressionRepository.deleteByUserId(userId);
        roleInferenceRepository.deleteByUserId(userId);
        recommendationResultRepository.deleteByUserId(userId);
        repositoryRepository.deleteByUserId(userId);
        repoSummaryRepository.deleteByUserId(userId);
        
        // Also delete by email (for legacy data)
        String email = user.getEmail();
        analysisResultRepository.deleteByUserId(email);
        skillProfileRepository.deleteByUserId(email);
        skillProgressionRepository.deleteByUserId(email);
        roleInferenceRepository.deleteByUserId(email);
        recommendationResultRepository.deleteByUserId(email);
        repositoryRepository.deleteByUserId(email);
        repoSummaryRepository.deleteByUserId(email);
        
        // Finally delete the user
        userRepository.delete(user);
        
        log.info("Account deleted successfully for user: {}", email);
    }
}
