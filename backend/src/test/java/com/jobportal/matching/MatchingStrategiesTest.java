package com.jobportal.matching;

import com.jobportal.model.Candidate;
import com.jobportal.model.Job;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.jobportal.TestData.candidate;
import static com.jobportal.TestData.job;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchingStrategiesTest {

    private static final double DELTA = 0.001;

    @Nested
    @DisplayName("SkillMatchingStrategy - TODO(#3)")
    class Skills {
        private final SkillMatchingStrategy strategy = new SkillMatchingStrategy();

        @Test
        void exampleFromTheSpec() {
            Job job = job(1, 1, 1, "Backend", "Java", "Spring Boot", "SQL", "Docker", "AWS");
            Candidate c = candidate(1, "Rajan", "Java", "Spring Boot", "SQL", "React");
            assertEquals(60.0, strategy.score(c, job), DELTA);
        }

        @Test
        void comparisonIsCaseInsensitive() {
            Job job = job(1, 1, 1, "Backend", "Java", "SQL");
            Candidate c = candidate(1, "A", "JAVA", " sql ");
            assertEquals(100.0, strategy.score(c, job), DELTA);
        }

        @Test
        void noOverlapScoresZero() {
            Job job = job(1, 1, 1, "Backend", "Java", "SQL");
            Candidate c = candidate(1, "A", "Python");
            assertEquals(0.0, strategy.score(c, job), DELTA);
        }

        @Test
        void candidateWithNoSkillsScoresZero() {
            Job job = job(1, 1, 1, "Backend", "Java");
            assertEquals(0.0, strategy.score(candidate(1, "A"), job), DELTA);
        }

        @Test
        void jobWithNoRequiredSkillsMatchesEveryone() {
            Job job = job(1, 1, 1, "Anything");
            assertEquals(100.0, strategy.score(candidate(1, "A", "Java"), job), DELTA);
        }
    }

    @Nested
    @DisplayName("SalaryMatchingStrategy - TODO(#4)")
    class Salary {
        private final SalaryMatchingStrategy strategy = new SalaryMatchingStrategy();

        @ParameterizedTest(name = "expected {0} vs max 10L -> {1}")
        @CsvSource({
                "300000, 100.0",    // below range
                "800000, 100.0",    // inside range
                "1000000, 100.0",   // exactly max
                "1200000, 80.0",    // 20% above max
                "1050000, 95.0",    // 5% above max
                "2000000, 0.0",     // 100% above max
                "2500000, 0.0",     // never negative
        })
        void scoresAgainstMax(long expected, double score) {
            Job job = job(1, 1, 1, "Backend"); // range 5L-10L
            Candidate c = candidate(1, "A");
            c.setExpectedSalary(expected);
            assertEquals(score, strategy.score(c, job), DELTA);
        }

        @Test
        void undisclosedSalaryMatchesEveryone() {
            Job job = job(1, 1, 1, "Backend");
            job.setSalaryRange(0, 0);
            Candidate c = candidate(1, "A");
            c.setExpectedSalary(5_000_000);
            assertEquals(100.0, strategy.score(c, job), DELTA);
        }
    }

    @Nested
    @DisplayName("ExperienceMatchingStrategy (implemented example)")
    class Experience {
        private final ExperienceMatchingStrategy strategy = new ExperienceMatchingStrategy();

        @ParameterizedTest(name = "{0} yrs vs 1-3 -> {1}")
        @CsvSource({"0, 75.0", "1, 100.0", "2, 100.0", "3, 100.0", "5, 80.0", "20, 50.0"})
        void scores(int years, double expected) {
            Candidate c = candidate(1, "A");
            c.setExperienceYears(years);
            assertEquals(expected, strategy.score(c, job(1, 1, 1, "Backend")), DELTA);
        }
    }

    @Nested
    @DisplayName("WeightedMatchingStrategy (implemented example)")
    class Weighted {
        @Test
        void computesWeightedAverage() {
            MatchingStrategy always80 = (c, j) -> 80.0;
            MatchingStrategy always20 = (c, j) -> 20.0;
            var weighted = new WeightedMatchingStrategy().with(always80, 3).with(always20, 1);
            // (80*3 + 20*1) / 4 = 65
            assertEquals(65.0, weighted.score(candidate(1, "A"), job(1, 1, 1, "X")), DELTA);
        }
    }
}
