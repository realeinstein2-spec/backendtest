package com.makershub.repository;

import com.makershub.entity.User;
import com.makershub.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@org.junit.jupiter.api.Disabled("Requires running Docker Desktop/daemon to run Testcontainers")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByPhoneNumber() {
        User user = User.builder()
                .phoneNumber("+233241234567")
                .passwordHash("hash")
                .fullName("Ama Owusu")
                .role(UserRole.SME_OWNER)
                .isVerified(true)
                .isActive(true)
                .ratingAvg(0.0)
                .totalOrders(0)
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByPhoneNumberAndDeletedAtIsNull("+233241234567");
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Ama Owusu");
    }
}
