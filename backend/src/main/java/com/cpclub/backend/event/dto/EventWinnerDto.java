package com.cpclub.backend.event.dto;

import com.cpclub.backend.event.entity.EventWinner;
import com.cpclub.backend.user.entity.User;

/**
 * One placing on an event's podium, as the public page renders it.
 *
 * <p>Carries the member's id so the page can link to their profile. Everything
 * else here is the same information the members page already shows publicly --
 * name, avatar, handle, rating -- read live from the member rather than copied
 * at the time they won, so a renamed member or a new avatar is right everywhere
 * at once.</p>
 *
 * <p>No email, and no phone number. A podium is not a reason to publish contact
 * details that the member's own profile would have withheld.</p>
 *
 * @param position 1, 2 or 3
 */
public record EventWinnerDto(
        Integer position,
        Long userId,
        String name,
        String avatarUrl,
        String codeforcesHandle,
        Integer rating
) {

    public static EventWinnerDto fromEntity(EventWinner winner) {
        User user = winner.getUser();
        return new EventWinnerDto(
                winner.getPosition(),
                user.getId(),
                user.getName(),
                user.getAvatarUrl(),
                user.getCodeforcesHandle(),
                user.getRating()
        );
    }
}
