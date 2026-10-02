package com.jobportal.service;

import com.jobportal.matching.MatchResult;
import com.jobportal.matching.MatchingStrategy;
import com.jobportal.model.Candidate;
import com.jobportal.model.Job;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.jobportal.TestData.candidate;
import static com.jobportal.TestData.job;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TODO(#5) rankCandidates and TODO(#6) topCandidates. */
class MatchingServiceTest {

    // rankCandidates/topCandidates don't touch the repositories, so nulls are fine here.
    private final MatchingService service = new MatchingService(null, null, null, null);
    private final Job job = job(1, 1, 1, "Backend");

    private final Candidate a = candidate(1, "Aarav");
    private final Candidate b = candidate(2, "Bhavna");
    private final Candidate c = candidate(3, "Chetan");
    private final Candidate d = candidate(4, "Divya");

    /** A fake strategy with fixed scores keeps these tests independent of TODO #3/#4. */
    private final MatchingStrategy fixedScores = (candidate, j) -> Map.of(
            "Aarav", 40.0, "Bhavna", 90.0, "Chetan", 60.0, "Divya", 75.0).get(candidate.getName());

    @Test
    void ranksBestFirst() {
        List<MatchResult> ranked = service.rankCandidates(job, List.of(a, b, c, d), fixedScores);

        assertEquals(List.of("Bhavna", "Divya", "Chetan", "Aarav"), names(ranked));
        assertEquals(90.0, ranked.getFirst().score(), 0.001);
    }

    @Test
    void tiesAreBrokenByName() {
        MatchingStrategy allEqual = (candidate, j) -> 50.0;
        assertEquals(List.of("Aarav", "Bhavna", "Chetan", "Divya"),
                names(service.rankCandidates(job, List.of(d, b, c, a), allEqual)));
    }

    @Test
    void emptyInputGivesEmptyRanking() {
        assertTrue(service.rankCandidates(job, List.of(), fixedScores).isEmpty());
    }

    @Test
    void topCandidatesReturnsBestNInOrder() {
        assertEquals(List.of("Bhavna", "Divya"), names(service.topCandidates(job, List.of(a, b, c, d), 2, fixedScores)));
    }

    @Test
    void topCandidatesWithNLargerThanInput() {
        assertEquals(4, service.topCandidates(job, List.of(a, b, c, d), 10, fixedScores).size());
    }

    @Test
    void topCandidatesWithZero() {
        assertTrue(service.topCandidates(job, List.of(a, b, c, d), 0, fixedScores).isEmpty());
    }

    @Test
    void topCandidatesAgreesWithFullRanking() {
        List<Candidate> many = IntStream.range(0, 500).mapToObj(i -> candidate(i, "C" + i)).toList();
        MatchingStrategy pseudoRandom = (candidate, j) -> (candidate.getId() * 37 % 101);

        List<MatchResult> fullTop10 = service.rankCandidates(job, many, pseudoRandom).subList(0, 10);
        assertEquals(names(fullTop10), names(service.topCandidates(job, many, 10, pseudoRandom)));
    }

    private static List<String> names(List<MatchResult> results) {
        return results.stream().map(r -> r.candidate().getName()).toList();
    }
}
