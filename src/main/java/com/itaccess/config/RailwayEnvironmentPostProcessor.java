package com.itaccess.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class RailwayEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getProperty("SPRING_DATASOURCE_URL") != null) {
            return;
        }

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            return;
        }

        try {
            URI uri = new URI(databaseUrl.trim());
            String host = uri.getHost();
            String userInfo = uri.getUserInfo();
            if (host == null || host.isBlank()) {
                return;
            }

            String username = null;
            String password = null;
            if (userInfo != null && !userInfo.isBlank()) {
                int idx = userInfo.indexOf(':');
                if (idx >= 0) {
                    username = userInfo.substring(0, idx);
                    password = userInfo.substring(idx + 1);
                } else {
                    username = userInfo;
                }
            }

            int port = uri.getPort();
            if (port <= 0) {
                port = 5432;
            }

            String path = uri.getPath();
            String database = (path == null || path.isBlank()) ? "railway" : (path.startsWith("/") ? path.substring(1) : path);
            if (database.isBlank()) {
                database = "railway";
            }

            Map<String, Object> map = new HashMap<>();
            map.put("spring.datasource.url", "jdbc:postgresql://" + host + ":" + port + "/" + database);
            map.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            if (username != null) {
                map.put("spring.datasource.username", username);
            }
            if (password != null) {
                map.put("spring.datasource.password", password);
            }
            environment.getPropertySources().addFirst(new MapPropertySource("railway-database-config", map));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Impossible de parser DATABASE_URL pour Railway : " + databaseUrl, e);
        }
    }
}
