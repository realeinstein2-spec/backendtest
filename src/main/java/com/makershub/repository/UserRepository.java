package com.makershub.repository;

import com.makershub.entity.User;
import com.makershub.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByPhoneNumberAndDeletedAtIsNull(String phoneNumber);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByPhoneNumber(String phoneNumber);

    long countByRoleAndIsVerifiedTrue(UserRole role);
}
