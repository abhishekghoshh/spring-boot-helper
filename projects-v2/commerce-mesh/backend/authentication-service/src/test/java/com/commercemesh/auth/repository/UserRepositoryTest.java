package com.commercemesh.auth.repository;

import com.commercemesh.auth.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("findByUsername")
    class FindByUsernameTests {

        @Test
        @DisplayName("should find user by username")
        void saveAndFindByUsername() {
            User user = createUser("finduser", "find@example.com");
            entityManager.persistAndFlush(user);

            Optional<User> found = userRepository.findByUsername("finduser");

            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("finduser");
            assertThat(found.get().getEmail()).isEqualTo("find@example.com");
        }

        @Test
        @DisplayName("should return empty Optional for non-existent username")
        void findByNonExistentUsernameReturnsEmpty() {
            Optional<User> found = userRepository.findByUsername("nonexistent");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmailTests {

        @Test
        @DisplayName("should find user by email")
        void saveAndFindByEmail() {
            User user = createUser("emailuser", "emailtest@example.com");
            entityManager.persistAndFlush(user);

            Optional<User> found = userRepository.findByEmail("emailtest@example.com");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("emailtest@example.com");
        }

        @Test
        @DisplayName("should return empty Optional for non-existent email")
        void findByNonExistentEmailReturnsEmpty() {
            Optional<User> found = userRepository.findByEmail("noone@example.com");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByUsername")
    class ExistsByUsernameTests {

        @Test
        @DisplayName("should return true when username exists")
        void existsByUsernameReturnsTrue() {
            User user = createUser("existinguser", "existing@example.com");
            entityManager.persistAndFlush(user);

            boolean exists = userRepository.existsByUsername("existinguser");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when username does not exist")
        void existsByUsernameReturnsFalse() {
            boolean exists = userRepository.existsByUsername("ghostuser");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmailTests {

        @Test
        @DisplayName("should return true when email exists")
        void existsByEmailReturnsTrue() {
            User user = createUser("emailcheck", "registered@example.com");
            entityManager.persistAndFlush(user);

            boolean exists = userRepository.existsByEmail("registered@example.com");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("should return false when email does not exist")
        void existsByEmailReturnsFalse() {
            boolean exists = userRepository.existsByEmail("free@example.com");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findByVerificationToken")
    class FindByVerificationTokenTests {

        @Test
        @DisplayName("should find user by verification token")
        void findByVerificationTokenReturnsUser() {
            User user = createUser("verifyuser", "verify@example.com");
            user.setVerificationToken("verification-token-123");
            entityManager.persistAndFlush(user);

            Optional<User> found = userRepository.findByVerificationToken("verification-token-123");

            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("verifyuser");
            assertThat(found.get().getVerificationToken()).isEqualTo("verification-token-123");
        }

        @Test
        @DisplayName("should return empty Optional when verification token not found")
        void findByNonExistentVerificationTokenReturnsEmpty() {
            Optional<User> found = userRepository.findByVerificationToken("no-such-token");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByResetToken")
    class FindByResetTokenTests {

        @Test
        @DisplayName("should find user by reset token")
        void findByResetTokenReturnsUser() {
            User user = createUser("resetuser", "reset@example.com");
            user.setResetToken("reset-token-abc");
            entityManager.persistAndFlush(user);

            Optional<User> found = userRepository.findByResetToken("reset-token-abc");

            assertThat(found).isPresent();
            assertThat(found.get().getResetToken()).isEqualTo("reset-token-abc");
        }

        @Test
        @DisplayName("should return empty Optional when reset token not found")
        void findByNonExistentResetTokenReturnsEmpty() {
            Optional<User> found = userRepository.findByResetToken("invalid-reset-token");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("should persist user with all fields set correctly")
        void saveUserWithAllFields() {
            User user = createUser("fulluser", "full@example.com");
            user.setFullName("Full Name");
            user.getRoles().add("ADMIN");

            User saved = userRepository.save(user);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getUsername()).isEqualTo("fulluser");
            assertThat(saved.getEmail()).isEqualTo("full@example.com");
            assertThat(saved.getFullName()).isEqualTo("Full Name");
            assertThat(saved.getRoles()).contains("ADMIN");
            assertThat(saved.isEmailVerified()).isFalse();
        }
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("securePassword123");
        user.getRoles().add("CUSTOMER");
        return user;
    }
}
