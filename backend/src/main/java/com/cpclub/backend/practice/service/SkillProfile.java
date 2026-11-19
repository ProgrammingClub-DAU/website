package com.cpclub.backend.practice.service;

import java.util.List;
import java.util.Map;

/**
 * A member's estimated Codeforces level, overall and per topic, on the rating
 * scale: a level of 1400 means problems rated about 1400 are solved about half
 * the time. Built by {@link SkillProfileService}.
 *
 * @param overallLevel the member's level across all topics
 * @param topicLevels  the level per core tag, for every core tag
 * @param topicSolves  rated problems solved per core tag
 * @param ratedSolves  distinct rated problems solved
 * @param weakTags     up to three topics to focus on, weakest first; empty until
 *                     there are enough solves to tell
 */
public record SkillProfile(int overallLevel,
                           Map<String, Integer> topicLevels,
                           Map<String, Integer> topicSolves,
                           int ratedSolves,
                           List<String> weakTags) {

    /** The level for a topic; the overall level for a tag outside the core set. */
    public int levelFor(String tag) {
        return topicLevels.getOrDefault(tag, overallLevel);
    }

    /**
     * The chance of solving a problem of this rating at this level, from
     * Codeforces' own Elo curve: a problem's rating is the rating of contestants
     * who solve it half the time, and every 400 points multiplies the odds by ten.
     */
    public static double chance(int problemRating, int level) {
        return 1.0 / (1.0 + Math.pow(10.0, (problemRating - level) / 400.0));
    }

    /** The problem rating at which {@link #chance} equals {@code chance}: the inverse of that curve. */
    public static int ratingWithChance(int level, double chance) {
        return (int) Math.round(level + 400.0 * Math.log10(1.0 / chance - 1.0));
    }
}
