package com.jobportal.api;

import com.jobportal.DemoData;
import com.jobportal.PortalApplication;
import com.jobportal.api.controller.AdminController;
import com.jobportal.api.controller.ApplicationController;
import com.jobportal.api.controller.AuthController;
import com.jobportal.api.controller.CompanyController;
import com.jobportal.api.controller.InterviewController;
import com.jobportal.api.controller.JobController;
import com.jobportal.api.controller.NotificationController;
import com.jobportal.api.controller.ProfileController;
import com.jobportal.api.dto.Views;
import com.jobportal.api.http.ApiHttpHandler;
import com.jobportal.api.http.Router;
import com.jobportal.config.ConfigurationManager;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * REST API entry point, built on the JDK's own {@code com.sun.net.httpserver} (no framework).
 *
 * <pre>./mvnw compile exec:java   ->   http://localhost:8080/api/health</pre>
 */
public final class ApiServer {

    private static final Logger log = LoggerFactory.getLogger(ApiServer.class);

    private ApiServer() {
    }

    public static void main(String[] args) throws IOException {
        ConfigurationManager config = ConfigurationManager.getInstance();
        int port = config.getInt("server.port", 8080);
        String corsOrigin = config.getString("server.cors.origin", "http://localhost:5173");

        Clock clock = Clock.systemDefaultZone();
        PortalApplication app = new PortalApplication(clock);
        app.start();
        if (Boolean.parseBoolean(config.getString("demo.data.enabled", "true"))) {
            DemoData.seed(app, clock);
        }

        Views views = new Views(app.users, app.jobs, app.companies, clock);
        Router router = new Router();
        router.get("/api/health", req -> Map.of("status", "UP"));
        new AuthController(app, views).register(router);
        new CompanyController(app, views).register(router);
        new ProfileController(app, views).register(router);
        new JobController(app, views).register(router);
        new ApplicationController(app, views).register(router);
        new InterviewController(app, views).register(router);
        new NotificationController(app, views).register(router);
        new AdminController(app, views).register(router);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new ApiHttpHandler(router, app.authService, corsOrigin));
        // Java 21 virtual threads: one cheap thread per request.
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down");
            server.stop(1);
            app.close();
        }));
        log.info("API listening on http://localhost:{}/api (CORS origin {})", port, corsOrigin);
    }
}
