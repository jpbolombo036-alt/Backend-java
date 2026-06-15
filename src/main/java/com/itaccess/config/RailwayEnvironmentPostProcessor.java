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
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            return;
        }

        try {
            URI uri = new URI(databaseUrl);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String path = uri.getPath();
            String database = path.startsWith("/") ? path.substring(1) : path;
            String username = uri.getUserInfo() != null ? uri.getUserInfo().split(":")[0] : null;
            String password = uri.getUserInfo() != null && uri.getUserInfo().contains(":") ? uri.getUserInfo().split(":", 2)[1] : null;

            if (database != null && !database.isEmpty()) {
                Map<String, Object> map = new HashMap<>();
                map.put("spring.datasource.url", "jdbc:postgresql://" + host + ":" + port + "/" + database);
                if (username != null) {
                    map.put("spring.datasource.username", username);
                }
                if (password != null) {
                    map.put("spring.datasource.password", password);
                }
                environment.getPropertySources().addFirst(new MapPropertySource("railway-database-config", map));
            }
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Impossible de parser DATABASE_URL pour Railway : " + databaseUrl, e);
        }
    }
}
