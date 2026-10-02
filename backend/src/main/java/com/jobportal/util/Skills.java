package com.jobportal.util;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Skill names are compared case-insensitively ("Java" == "java" == " JAVA ").
 * Normalizing once on the way in keeps every comparison downstream a plain HashSet lookup.
 */
public final class Skills {

    private Skills() {
    }

    public static Set<String> normalize(Collection<String> skills) {
        if (skills == null) return Set.of();
        return skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    public static String normalize(String skill) {
        return skill.trim().toLowerCase(Locale.ROOT);
    }
}
