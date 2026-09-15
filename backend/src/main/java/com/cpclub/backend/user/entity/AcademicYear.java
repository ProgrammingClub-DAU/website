package com.cpclub.backend.user.entity;

/**
 * How far into the course a member is.
 *
 * <p>Two values rather than a batch year or a numeric level, because the club
 * only ever asks in order to tell first-years from everyone else: first-years
 * get the beginner sessions, everyone else gets the contest track.</p>
 *
 * <p>Deliberately not derived from {@code batchYear}. A stored year silently
 * becomes wrong the moment the academic year rolls over, whereas this is a
 * statement the member makes and can correct on their own profile.</p>
 */
public enum AcademicYear {

    /** In the first year of the course. */
    FIRST_YEAR,

    /** Second year or later, including final-year and postgraduate members. */
    SECOND_YEAR_ONWARDS
}
