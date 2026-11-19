package com.cpclub.backend.practice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cpclub.backend.practice.service.TopicStrengthService.CORE_TAGS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** The level estimates and the chance curve, without a database. */
class SkillProfileServiceTest {

    @Test
    @DisplayName("A level is the mean of the hardest solves; missing ones count as the floor")
    void sampleLevel_hardestSolvesOnly() {
        assertThat(SkillProfileService.sampleLevel(List.of(1500, 1400, 1300, 1200, 1100, 800, 800), 5, 800))
                .isEqualTo(1300);
        assertThat(SkillProfileService.sampleLevel(List.of(1600), 5, 1000)).isEqualTo(1120);
        assertThat(SkillProfileService.sampleLevel(List.of(), 10, 800)).isEqualTo(800);
    }

    @Test
    @DisplayName("Solving easy problems never lowers a topic level")
    void easySolves_neverLowerALevel() {
        int untouched = SkillProfileService.topicLevel(List.of(), 1500);
        int withEasySolves = SkillProfileService.topicLevel(List.of(900, 800, 800), 1500);

        assertThat(untouched).isEqualTo(1100);
        assertThat(withEasySolves).isEqualTo(untouched);
        assertThat(SkillProfileService.topicLevel(List.of(1700, 1600, 1500, 1500, 1500), 1500)).isEqualTo(1560);
    }

    @Test
    @DisplayName("Many easy solves do not raise a level past the hardest problems solved")
    void manyEasySolves_doNotInflate() {
        List<Integer> hundredEasy = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) hundredEasy.add(800);
        hundredEasy.add(1200);

        assertThat(SkillProfileService.overallLevel(null, hundredEasy)).isEqualTo(840);
    }

    @Test
    @DisplayName("A contest rating is raised halfway toward harder practice, never lowered")
    void overallLevel_blendsRatingAndPractice() {
        List<Integer> tenAt1600 = List.of(1600, 1600, 1600, 1600, 1600, 1600, 1600, 1600, 1600, 1600);
        assertThat(SkillProfileService.overallLevel(1400, tenAt1600)).isEqualTo(1500);
        assertThat(SkillProfileService.overallLevel(1800, tenAt1600)).isEqualTo(1800);
        assertThat(SkillProfileService.overallLevel(null, tenAt1600)).isEqualTo(1600);
        assertThat(SkillProfileService.overallLevel(0, List.of())).isEqualTo(800);
    }

    @Test
    @DisplayName("Focus topics are the weakest of those common near the member's level")
    void weakTags_skipRareTopics() {
        Map<String, Integer> levels = new HashMap<>();
        CORE_TAGS.forEach(tag -> levels.put(tag, 1500));
        levels.put("geometry", 900);   // weakest, but rare near this level
        levels.put("dp", 1100);
        levels.put("graphs", 1200);
        levels.put("math", 1300);
        levels.put("strings", 1300);

        Map<String, Double> shares = new HashMap<>();
        CORE_TAGS.forEach(tag -> shares.put(tag, 0.10));
        shares.put("geometry", 0.02);
        shares.put("strings", 0.05);   // ties math on level, less common

        assertThat(SkillProfileService.weakTags(levels, shares)).containsExactly("dp", "graphs", "math");
    }

    @Test
    @DisplayName("The chance curve is Codeforces' Elo curve, and ratingWithChance inverts it")
    void chanceCurve() {
        assertThat(SkillProfile.chance(1400, 1400)).isCloseTo(0.5, within(1e-9));
        assertThat(SkillProfile.chance(1800, 1400)).isCloseTo(1.0 / 11, within(1e-9));
        assertThat(SkillProfile.ratingWithChance(1150, 0.70)).isEqualTo(1003);
        assertThat(SkillProfile.ratingWithChance(1150, 0.40)).isEqualTo(1220);
        assertThat(SkillProfile.chance(SkillProfile.ratingWithChance(1500, 0.6), 1500)).isCloseTo(0.6, within(0.01));
    }
}
