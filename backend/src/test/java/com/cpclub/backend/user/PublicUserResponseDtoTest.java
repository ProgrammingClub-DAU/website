package com.cpclub.backend.user;

import com.cpclub.backend.user.dto.PublicUserResponseDto;
import com.cpclub.backend.user.entity.ClubRole;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Who may see a member's phone number.
 *
 * <p>This is the one field in the public projection that depends on who is
 * asking, and getting it wrong publishes several hundred students' phone
 * numbers, so it is pinned down here rather than left to the endpoint tests.</p>
 *
 * <p>The rule has two halves, from two different reasons. Someone holding a club
 * post is a published point of contact for that post. Everyone else gave their
 * number so an organiser could reach them about an event, which is a reason for
 * admins to have it and for nobody else to.</p>
 */
class PublicUserResponseDtoTest {

    private static final String NUMBER = "9876543210";

    @ParameterizedTest
    @EnumSource(value = ClubRole.class,
            names = {"CONVENOR", "DEPUTY_CONVENOR", "CORE", "ASSOCIATE_CORE", "BATCH_REPRESENTATIVE"})
    @DisplayName("An office bearer's number is public, because the post is a contact point")
    void officeBearersPublishTheirNumber(ClubRole post) {
        User user = member(post);

        assertEquals(NUMBER, PublicUserResponseDto.fromEntity(user, false).phoneNumber());
    }

    @ParameterizedTest
    @EnumSource(value = ClubRole.class, names = {"STUDENT", "EX_PC_MEMBER", "EX_CORE", "EX_CDC"})
    @DisplayName("Everyone else's number is withheld from the public, including past post-holders")
    void everyoneElseKeepsTheirNumberPrivate(ClubRole post) {
        User user = member(post);

        assertNull(PublicUserResponseDto.fromEntity(user, false).phoneNumber());
    }

    @Test
    @DisplayName("An unassigned member is not an office bearer")
    void aNullClubRoleIsNotAnOfficeBearer() {
        // Most accounts are in this state: signed in, never given a post. The
        // null has to be handled as "no post", not fall through to visible.
        User user = member(null);

        assertNull(PublicUserResponseDto.fromEntity(user, false).phoneNumber());
    }

    @Test
    @DisplayName("An admin sees any member's number, which is what attendance needs")
    void adminsSeeEveryNumber() {
        User user = member(ClubRole.STUDENT);

        assertEquals(NUMBER, PublicUserResponseDto.fromEntity(user, true).phoneNumber());
    }

    @Test
    @DisplayName("A member who never gave a number reports none rather than failing")
    void aMissingNumberIsSimplyAbsent() {
        User user = member(ClubRole.CONVENOR);
        user.setPhoneNumber(null);

        assertNull(PublicUserResponseDto.fromEntity(user, false).phoneNumber());
        assertNull(PublicUserResponseDto.fromEntity(user, true).phoneNumber());
    }

    @Test
    @DisplayName("The rest of the public profile is carried through for the members page")
    void carriesWhatTheMembersPageRenders() {
        User user = member(ClubRole.CORE);
        user.setAvatarUrl("https://cdn/ravi.png");
        user.setLinkedinUrl("https://www.linkedin.com/in/ravi");

        PublicUserResponseDto dto = PublicUserResponseDto.fromEntity(user, false);

        assertEquals("https://cdn/ravi.png", dto.avatarUrl());
        assertEquals(ClubRole.CORE, dto.clubRole());
        assertEquals("https://www.linkedin.com/in/ravi", dto.linkedinUrl());
        assertEquals("ravi_cf", dto.codeforcesHandle());
    }

    private User member(ClubRole post) {
        User user = new User("Ravi", "202401226@dau.ac.in", null, Role.ROLE_USER);
        user.setClubRole(post);
        user.setPhoneNumber(NUMBER);
        user.setCodeforcesHandle("ravi_cf");
        return user;
    }
}
