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
    CONVENOR(1, true),

    /** Second to the Convenor. */
    DEPUTY_CONVENOR(2, true),

    /** Core team member, running club operations. */
    CORE(3, true),

    /** Associate of the core team, typically a junior appointment. */
    ASSOCIATE_CORE(4, true),

    /** Represents a particular admission batch. */
    BATCH_REPRESENTATIVE(5, true),

    /** Former Programming Club member. */
    EX_PC_MEMBER(6, false),

    /** Former core team member. */
    EX_CORE(7, false),

    /** Former Career Development Cell member. */
    EX_CDC(8, false),

    /** Ordinary member holding no club post. */
    STUDENT(9, false);

    private final int hierarchyRank;
    private final boolean officeBearer;

    ClubRole(int hierarchyRank, boolean officeBearer) {
        this.hierarchyRank = hierarchyRank;
        this.officeBearer = officeBearer;
    }

    /**
     * Where this post sits in the club, lowest number first.
     *
     * <p>Stated explicitly rather than taken from {@link #ordinal()}. The two
     * agree today, but ordinal is a property of the declaration order, so
     * inserting a constant in the middle -- the obvious way to add a new post --
     * would silently reshuffle the public members page. This number is the
     * club's hierarchy, and changing it should be a decision, not a side effect.</p>
     *
     * @return the sort key for the members page
     */
    public int hierarchyRank() {
        return hierarchyRank;
    }

    /**
     * Whether this post is a current, public position in the club.
     *
     * <p>Drives two things that happen to coincide: who appears on the public
     * members page, and whose phone number is public. Both follow from the same
     * fact -- someone holding a club post is a point of contact for it, and is
     * listed as one. Past members and ordinary students are neither.</p>
     *
     * @return true for the five serving posts
     */
    public boolean isOfficeBearer() {
        return officeBearer;
    }
}
