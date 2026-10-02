package com.jobportal.matching;

import com.jobportal.model.Candidate;
import com.jobportal.model.Job;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Combines several strategies into a weighted average. This is a composite: it is itself a
 * MatchingStrategy, so it can be passed anywhere a single strategy can.
 *
 * <pre>
 * new WeightedMatchingStrategy()
 *     .with(new SkillMatchingStrategy(), 0.6)
 *     .with(new ExperienceMatchingStrategy(), 0.3)
 *     .with(new SalaryMatchingStrategy(), 0.1);
 * </pre>
 */
public class WeightedMatchingStrategy implements MatchingStrategy {

    private final Map<MatchingStrategy, Double> weights = new LinkedHashMap<>();

    public WeightedMatchingStrategy with(MatchingStrategy strategy, double weight) {
        if (weight <= 0) throw new IllegalArgumentException("weight must be positive");
        weights.merge(strategy, weight, Double::sum);
        return this;
    }

    @Override
    public double score(Candidate candidate, Job job) {
        if (weights.isEmpty()) throw new IllegalStateException("No strategies configured");
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double weighted = weights.entrySet().stream()
                .mapToDouble(e -> e.getKey().score(candidate, job) * e.getValue())
                .sum();
        return weighted / totalWeight;
    }
}
