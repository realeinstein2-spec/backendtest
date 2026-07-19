package com.makershub.repository;

import com.makershub.entity.User;
import com.makershub.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByPhoneNumberAndDeletedAtIsNull(String phoneNumber);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByPhoneNumber(String phoneNumber);

    long countByRoleAndIsVerifiedTrue(UserRole role);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Page<User> findByLastActiveAtAfterAndDeletedAtIsNullOrderByLastActiveAtDesc(Instant threshold, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL " +
            "AND (:role IS NULL OR u.role = :role) " +
            "AND (:isActive IS NULL OR u.isActive = :isActive) " +
            "AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR u.phoneNumber LIKE CONCAT('%', :search, '%'))")
    Page<User> findAllForAdmin(
            @Param("role") UserRole role,
            @Param("isActive") Boolean isActive,
            @Param("search") String search,
            Pageable pageable);
}
