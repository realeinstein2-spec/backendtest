package com.makershub.service;

import com.makershub.dto.request.FactoryRequest;
import com.makershub.dto.request.UserRequest;
import com.makershub.dto.response.AuthResponse;
import com.makershub.entity.Factory;
import com.makershub.entity.User;
import com.makershub.enums.AuditAction;
import com.makershub.enums.UserRole;
import com.makershub.enums.VerificationStatus;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.exception.UnauthorizedException;
import com.makershub.audit.AuditLogger;
import com.makershub.repository.FactoryRepository;
import com.makershub.repository.UserRepository;
import com.makershub.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FactoryRepository factoryRepository;
    private final AuditLogger auditLogger;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Transactional(readOnly = true)
    public AuthResponse.UserSummaryResponse getCurrentUser() {
        return toSummary(getAuthenticatedUser());
    }

    @Transactional
    public AuthResponse.UserSummaryResponse createFactoryProfile(FactoryRequest.CreateFactoryProfileRequest request) {
        User user = getAuthenticatedUser();
        if (user.getRole() != UserRole.FACTORY_OWNER) {
            throw new UnauthorizedException("Only factory owners can create a factory profile");
        }
        if (factoryRepository.findByUserId(user.getId()).isPresent()) {
            throw new BusinessException("Factory profile already exists", HttpStatus.CONFLICT, "FACTORY_EXISTS");
        }
        Point location = null;
        if (request.getLatitude() != null && request.getLongitude() != null) {
            location = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));
            location.setSRID(4326);
        }
        Factory factory = Factory.builder()
                .user(user)
                .companyName(request.getCompanyName())
                .description(request.getDescription())
                .sectorTags(request.getSectorTags())
                .machineryList(request.getMachineryList())
                .minOrderQuantity(request.getMinOrderQuantity())
                .maxOrderQuantity(request.getMaxOrderQuantity())
                .gpsCoordinates(location)
                .address(request.getAddress())
                .verificationStatus(VerificationStatus.PENDING)
                .isFeatured(false)
                .payoutAccountType(request.getPayoutAccountType())
                .payoutAccountName(request.getPayoutAccountName())
                .payoutAccountNumber(request.getPayoutAccountNumber())
                .payoutBankCode(request.getPayoutBankCode())
                .build();
        Factory saved = factoryRepository.save(factory);
        auditLogger.log(AuditAction.CREATE, "FACTORY", saved.getId(), null, null);

        // Update user profile fields if provided
        boolean userUpdated = false;
        if (request.getEmail() != null) { user.setEmail(request.getEmail()); userUpdated = true; }
        if (request.getRegion() != null) { user.setRegion(request.getRegion()); userUpdated = true; }
        if (request.getTown() != null) { user.setTown(request.getTown()); userUpdated = true; }
        if (request.getProfileImageUrl() != null) { user.setProfileImageUrl(request.getProfileImageUrl()); userUpdated = true; }
        if (userUpdated) {
            userRepository.save(user);
        }

        return toSummary(user);
    }

    @Transactional
    public AuthResponse.UserSummaryResponse updateFactoryProfile(FactoryRequest.CreateFactoryProfileRequest request) {
        User user = getAuthenticatedUser();
        Factory factory = factoryRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Factory", "User ID: " + user.getId()));

        factory.setCompanyName(request.getCompanyName());
        factory.setDescription(request.getDescription());
        factory.setSectorTags(request.getSectorTags());
        factory.setMachineryList(request.getMachineryList());
        factory.setMinOrderQuantity(request.getMinOrderQuantity());
        factory.setMaxOrderQuantity(request.getMaxOrderQuantity());
        factory.setAddress(request.getAddress());
        factory.setPayoutAccountType(request.getPayoutAccountType());
        factory.setPayoutAccountName(request.getPayoutAccountName());
        factory.setPayoutAccountNumber(request.getPayoutAccountNumber());
        factory.setPayoutBankCode(request.getPayoutBankCode());

        if (request.getLatitude() != null && request.getLongitude() != null) {
            Point location = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));
            location.setSRID(4326);
            factory.setGpsCoordinates(location);
        } else {
            factory.setGpsCoordinates(null);
        }

        factoryRepository.save(factory);
        auditLogger.log(AuditAction.UPDATE, "FACTORY", factory.getId(), null, null);

        // Update user profile fields if provided
        boolean userUpdated = false;
        if (request.getEmail() != null) { user.setEmail(request.getEmail()); userUpdated = true; }
        if (request.getRegion() != null) { user.setRegion(request.getRegion()); userUpdated = true; }
        if (request.getTown() != null) { user.setTown(request.getTown()); userUpdated = true; }
        if (request.getProfileImageUrl() != null) { user.setProfileImageUrl(request.getProfileImageUrl()); userUpdated = true; }
        if (userUpdated) {
            userRepository.save(user);
        }

        return toSummary(user);
    }

    @Transactional
    public AuthResponse.UserSummaryResponse updateProfile(UserRequest.UpdateProfileRequest request) {
        User user = getAuthenticatedUser();
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
        User saved = userRepository.save(user);
        auditLogger.log(AuditAction.UPDATE, "USER", saved.getId(), null, null);
        return toSummary(saved);
    }

    private User getAuthenticatedUser() {
        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId().toString()));
    }

    private AuthResponse.UserSummaryResponse toSummary(User user) {
        String payoutType = null;
        String payoutName = null;
        String payoutNumber = null;
        String payoutBank = null;
        if (user.getFactory() != null) {
            payoutType = user.getFactory().getPayoutAccountType();
            payoutName = user.getFactory().getPayoutAccountName();
            payoutNumber = user.getFactory().getPayoutAccountNumber();
            payoutBank = user.getFactory().getPayoutBankCode();
        }
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
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isActive(user.getIsActive())
                .payoutAccountType(payoutType)
                .payoutAccountName(payoutName)
                .payoutAccountNumber(payoutNumber)
                .payoutBankCode(payoutBank)
                .build();
    }
}
