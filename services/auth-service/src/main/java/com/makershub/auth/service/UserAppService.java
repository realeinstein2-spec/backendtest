package com.makershub.auth.service;

import com.makershub.auth.dto.AuthResponse;
import com.makershub.auth.dto.UserRequest;
import com.makershub.auth.entity.User;
import com.makershub.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserAppService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AuthResponse.UserSummaryResponse getUserById(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return mapToSummary(user);
    }

    @Transactional
    public AuthResponse.UserSummaryResponse updateProfile(UUID userId, UserRequest.UpdateProfileRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getRegion() != null) {
            user.setRegion(request.getRegion());
        }
        if (request.getTown() != null) {
            user.setTown(request.getTown());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }
        if (request.getCoverImageUrl() != null) {
            user.setCoverImageUrl(request.getCoverImageUrl());
        }
        User saved = userRepository.save(user);
        return mapToSummary(saved);
    }

    @Transactional
    public void updateFcmToken(UUID userId, UserRequest.UpdateFcmTokenRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setFcmToken(request.getFcmToken());
        userRepository.save(user);
    }

    private AuthResponse.UserSummaryResponse mapToSummary(User user) {
        return AuthResponse.UserSummaryResponse.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .email(user.getEmail())
                .ghanaCardNumber(user.getGhanaCardNumber())
                .region(user.getRegion())
                .town(user.getTown())
                .profileImageUrl(user.getProfileImageUrl())
                .coverImageUrl(user.getCoverImageUrl())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isActive(user.getIsActive())
                .build();
    }
}
