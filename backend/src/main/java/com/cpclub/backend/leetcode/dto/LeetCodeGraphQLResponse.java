package com.cpclub.backend.leetcode.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Shape of the LeetCode GraphQL reply for a single user's contest ranking.
 *
 * <p>The wire format is:</p>
 * <pre>
 * { "data": { "userContestRanking": { "rating": 1843.21 } } }
 * </pre>
 *
 * <p>Every level is nullable and every level means something different when it is
 * null, which is why this is three records rather than one flattened one:</p>
 * <ul>
 *   <li>{@code data} null — the request failed or was rejected.</li>
 *   <li>{@code userContestRanking} null — the handle is valid but the member has
 *       never entered a rated contest. LeetCode returns this rather than an
 *       error, so it is a successful sync with a rating of zero, not a failure.</li>
 *   <li>{@code rating} null — LeetCode changed the schema.</li>
 * </ul>
 *
 * <p>{@code rating} is a Double because LeetCode returns a fractional rating;
 * the column is an integer, so the service rounds.</p>
 *
 * <p>{@code @JsonIgnoreProperties} on each level is not decoration: the real
 * response carries other fields (attendedContestsCount, globalRanking,
 * topPercentage) and LeetCode adds more without warning. Failing on an unknown
 * field would turn a schema addition into a sync outage.</p>
 *
 * @param data the GraphQL payload wrapper
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeetCodeGraphQLResponse(Data data) {

    /**
     * The {@code data} envelope of the GraphQL response.
     *
     * @param userContestRanking contest standing, null when the member has never contested
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(UserContestRanking userContestRanking) {
    }

    /**
     * A member's contest standing.
     *
     * @param rating current contest rating, fractional on the wire
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserContestRanking(Double rating) {
    }

    /**
     * Extracts the rating, collapsing every null level into "no rating".
     *
     * <p>Callers cannot distinguish "never contested" from "malformed response"
     * through this method, and should not: both mean there is no rating to
     * record, and the service logs the difference where it matters.</p>
     *
     * @return the rating when present, otherwise null
     */
    public Double extractRating() {
        if (data == null || data.userContestRanking() == null) {
            return null;
        }
        return data.userContestRanking().rating();
    }

    /**
     * Whether the response is a well-formed reply for a member who has simply
     * never entered a rated contest.
     *
     * <p>Distinguished from a failure so the service can record zero — a real,
     * known value — instead of leaving the previous rating in place.</p>
     *
     * @return true when {@code data} arrived but carried no contest ranking
     */
    public boolean isUnratedMember() {
        return data != null && data.userContestRanking() == null;
    }
}
