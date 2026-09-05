package com.cpclub.backend.user.entity;

/**
 * A member's position within the CP Club.
 *
 * <p>This is deliberately separate from {@link Role}, which controls platform
 * permissions. {@code ClubRole} answers "what post does this member hold in the
 * club?"; {@code Role} answers "what may this account do through the API?".
 * Conflating them would hand every Batch Representative admin access.</p>
 *
 * <p>A {@code null} club role means unassigned, and renders as "Member" on the
 * frontend. {@link #STUDENT} is different: it is an explicit assignment meaning
 * the member holds no special position.</p>
 *
 * <p>Persisted as a string via {@code @Enumerated(EnumType.STRING)}. The names
 * below must stay in step with the {@code users_club_role_check} constraint in
 * {@code V2__extend_user_profile.sql} -- adding a constant here without a
 * migration will fail the insert at runtime, not at startup.</p>
 */
public enum ClubRole {
    /** Club head. */
    CONVENOR,

    /** Second to the Convenor. */
    DEPUTY_CONVENOR,

    /** Core team member, running club operations. */
    CORE,

    /** Associate of the core team, typically a junior appointment. */
    ASSOCIATE_CORE,

    /** Represents a particular admission batch. */
    BATCH_REPRESENTATIVE,

    /** Former Programming Club member. */
    EX_PC_MEMBER,

    /** Former core team member. */
    EX_CORE,

    /** Former Career Development Cell member. */
    EX_CDC,

    /** Ordinary member holding no club post. */
    STUDENT
}
