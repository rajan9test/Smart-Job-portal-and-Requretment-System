package com.jobportal.api.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps "METHOD /path/{var}" to handlers. Each path template is compiled to a regex once;
 * {@code {id}} becomes a named group capturing one path segment.
 *
 * This is a tiny version of what Spring's @GetMapping / @PathVariable do for you in Phase 3.
 */
public class Router {

    private record Route(String method, String template, Pattern pattern, List<String> variables, Handler handler) {
    }

    public record Match(Handler handler, Map<String, String> pathParams) {
    }

    private static final Pattern VARIABLE = Pattern.compile("\\{(\\w+)}");

    private final List<Route> routes = new ArrayList<>();

    public Router get(String template, Handler handler) {
        return add("GET", template, handler);
    }

    public Router post(String template, Handler handler) {
        return add("POST", template, handler);
    }

    public Router put(String template, Handler handler) {
        return add("PUT", template, handler);
    }

    public Router delete(String template, Handler handler) {
        return add("DELETE", template, handler);
    }

    private Router add(String method, String template, Handler handler) {
        List<String> variables = new ArrayList<>();
        Matcher m = VARIABLE.matcher(template);
        StringBuilder regex = new StringBuilder();
        int last = 0;
        while (m.find()) {
            regex.append(Pattern.quote(template.substring(last, m.start())));
            regex.append("([^/]+)");
            variables.add(m.group(1));
            last = m.end();
        }
        regex.append(Pattern.quote(template.substring(last)));
        routes.add(new Route(method, template, Pattern.compile(regex.toString()), variables, handler));
        return this;
    }

    /**
     * Finds the handler for a request. Literal routes are registered before variable ones where they
     * could clash (e.g. /api/jobs/mine before /api/jobs/{id}), and the first match wins.
     *
     * @return empty if no route matches the path at all; throws 405 if the path exists for another method
     */
    public Optional<Match> match(String method, String path) {
        boolean pathExists = false;
        for (Route route : routes) {
            Matcher m = route.pattern().matcher(path);
            if (!m.matches()) continue;
            pathExists = true;
            if (!route.method().equals(method)) continue;
            Map<String, String> params = new HashMap<>();
            for (int i = 0; i < route.variables().size(); i++) {
                params.put(route.variables().get(i), m.group(i + 1));
            }
            return Optional.of(new Match(route.handler(), params));
        }
        if (pathExists) {
            throw new ApiException(405, method + " not allowed on " + path);
        }
        return Optional.empty();
    }
}
