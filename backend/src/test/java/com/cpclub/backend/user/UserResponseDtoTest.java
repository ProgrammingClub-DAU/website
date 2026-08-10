package com.cpclub.backend.user;

import com.cpclub.backend.user.dto.UserResponseDto;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
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
        assertEquals("alice@example.com", dto.email());
        assertEquals("tourist_fan", dto.codeforcesHandle());
        assertEquals(1500, dto.rating());
        assertEquals(Role.ROLE_USER, dto.role());
    }
}
