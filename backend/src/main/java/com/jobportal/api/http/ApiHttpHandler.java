package com.jobportal.api.http;

import com.jobportal.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Front controller: every request under /api passes through here.
 * route lookup -> handler -> JSON serialization, with errors mapped by {@link ExceptionMapper}.
 */
public class ApiHttpHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiHttpHandler.class);

    private final Router router;
    private final AuthService auth;
    private final String allowedOrigin;

    public ApiHttpHandler(Router router, AuthService auth, String allowedOrigin) {
        this.router = router;
        this.auth = auth;
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long started = System.nanoTime();
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        int status;
        try {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(method)) { // CORS preflight
                status = 204;
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            Object result;
            try {
                Router.Match match = router.match(method, path)
                        .orElseThrow(() -> new ApiException(404, "No endpoint " + method + " " + path));
                result = match.handler().handle(new Request(exchange, match.pathParams(), auth));
            } catch (Exception e) {
                ExceptionMapper.ErrorBody error = ExceptionMapper.map(e);
                result = new Response(error.status(), error);
            }
            Response response = result instanceof Response r ? r
                    : result == null ? Response.noContent()
                    : new Response(200, result);
            status = response.status();
            write(exchange, response);
        } finally {
            exchange.close();
        }
        log.info("{} {} -> {} ({} ms)", method, path, status, (System.nanoTime() - started) / 1_000_000);
    }

    private void write(HttpExchange exchange, Response response) throws IOException {
        if (response.body() == null || response.status() == 204) {
            exchange.sendResponseHeaders(response.status(), -1);
            return;
        }
        byte[] bytes = Json.mapper().writeValueAsBytes(response.body());
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(response.status(), bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        var headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", allowedOrigin);
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
