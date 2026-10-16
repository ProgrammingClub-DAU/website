package com.cpclub.backend.contest.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * The parts of a Codeforces {@code contest.standings} result that club contests use.
 *
 * <p>Field names follow the API exactly, and are pinned by the captured response in
 * {@code src/test/resources/fixtures/cf-contest-standings.json}. Everything the club
 * does not read is ignored rather than mapped, so an added field upstream cannot
 * break deserialisation.</p>
 *
 * <p>{@code rows} holds only the rows the caller asked for. For an official contest
 * that is a filtered subset of tens of thousands of rows, chosen while the response
 * is still streaming -- see {@link CodeforcesAuthorizedClient}.</p>
 */
@Data
public class CfStandings {

    private Contest contest;
    private List<Problem> problems = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contest {
        private Integer id;
        private String name;
        /** CF, IOI or ICPC. */
        private String type;
        private String phase;
        private Long durationSeconds;
        private Long startTimeSeconds;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Problem {
        private Integer contestId;
        private String index;
        private String name;
        private Integer rating;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {
        private Party party;
        private Integer rank;
        private Double points;
        private Integer penalty;
        /** One entry per problem, in the same order as {@link CfStandings#problems}. */
        private List<ProblemResult> problemResults = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Party {
        private Integer contestId;
        private List<Member> members = new ArrayList<>();
        /** CONTESTANT, PRACTICE, VIRTUAL, MANAGER or OUT_OF_COMPETITION. */
        private String participantType;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Member {
        private String handle;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProblemResult {
        private Double points;
        private Integer rejectedAttemptCount;
        private String type;
        private Integer bestSubmissionTimeSeconds;

        /**
         * Solved means points were awarded. In ICPC rules that is exactly 1.0 for an
         * accepted problem; in CF rules it is the problem's decayed score. An IOI
         * contest can award partial points, which this counts as solved -- the club's
         * contests are ICPC-style mashups, so that case is accepted rather than
         * special-cased.
         */
        public boolean isSolved() {
            return points != null && points > 0;
        }

        public boolean isAttempted() {
            return isSolved() || (rejectedAttemptCount != null && rejectedAttemptCount > 0);
        }
    }
}
