package com.cpclub.backend.user;

import com.cpclub.backend.entity.Role;
import com.cpclub.backend.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserResponseDtoTest {

    @Test
    @DisplayName("Should correctly map User entity to UserResponseDto without exposing password")
    void shouldMapUserEntityToUserResponseDto() {
        User user = new User("Alice Smith", "alice@example.com", "secret_bcrypt_hash", Role.ROLE_USER);
        user.setId(42L);
        user.setCodeforcesHandle("tourist_fan");
        user.setRating(1500);

        UserResponseDto dto = UserResponseDto.fromEntity(user);

        assertNotNull(dto);
        assertEquals(42L, dto.id());
        assertEquals("Alice Smith", dto.name());
        assertEquals("tourist_fan", dto.codeforcesHandle());
        assertEquals(1500, dto.rating());
        assertEquals(Role.ROLE_USER, dto.role());
    }

    @Test
    @DisplayName("Should return null when mapping null User entity")
    void shouldReturnNullForNullUserEntity() {
        assertNull(UserResponseDto.fromEntity(null));
    }
}
