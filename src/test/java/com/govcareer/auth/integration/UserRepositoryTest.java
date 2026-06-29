package com.govcareer.auth.integration;

import com.govcareer.auth.entity.Role;
import com.govcareer.auth.entity.User;
import com.govcareer.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UserRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindByEmail() {
        User user = User.builder()
                .email("admin@govcareer.com")
                .password("securePassword123")
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .build();

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("admin@govcareer.com");

        assertTrue(found.isPresent());
        assertEquals("Admin", found.get().getFirstName());
        assertEquals("User", found.get().getLastName());
        assertEquals(Role.ADMIN, found.get().getRole());
    }
    
    @Test
    void testExistsByEmail() {
        User user = User.builder()
                .email("test.exists@govcareer.com")
                .password("securePassword123")
                .firstName("Test")
                .lastName("Exists")
                .role(Role.USER)
                .build();

        userRepository.save(user);
        
        assertTrue(userRepository.existsByEmail("test.exists@govcareer.com"));
        assertFalse(userRepository.existsByEmail("does.not.exist@govcareer.com"));
    }
}
