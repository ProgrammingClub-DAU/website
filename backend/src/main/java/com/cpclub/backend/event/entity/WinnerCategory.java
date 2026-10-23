package com.cpclub.backend.event.entity;

import com.cpclub.backend.user.entity.AcademicYear;

/**
 * Which podium a placing belongs to.
 *
 * <p>The club awards separate prizes to first-year students and to everyone from
 * second year on, so an event such as an IPC can have a 1st-3rd place in each
 * division. {@link #OVERALL} is a single podium for the whole event, which is what
 * every placing recorded before divisions existed is.</p>
 */
public enum WinnerCategory {
    OVERALL,
    FIRST_YEAR,
    SECOND_YEAR_ONWARDS;

    /** The academic year a member must be in to place in this division, or null for {@link #OVERALL}. */
    public AcademicYear requiredYear() {
        return switch (this) {
            case OVERALL -> null;
            case FIRST_YEAR -> AcademicYear.FIRST_YEAR;
            case SECOND_YEAR_ONWARDS -> AcademicYear.SECOND_YEAR_ONWARDS;
        };
    }
}
