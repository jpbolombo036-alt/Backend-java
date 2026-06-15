package com.itaccess.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RailwayEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Pattern DATABASE_URL_PATTERN =
            Pattern.compile("^postgresql://([^:]+):([^@]+)@([^:]+):(\\d+)/(.+)$");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getProperty("spring.datasource.url") != null
                || environment.getProperty("SPRING_DATASOURCE_URL") != null) {
            return;
        }

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            return;
        }

        Matcher matcher = DATABASE_URL_PATTERN.matcher(databaseUrl.trim());
        if (!matcher.find()) {
            throw new IllegalStateException(
                    "Format DATABASE_URL Railway invalide : " + databaseUrl + ". Format attendu : postgresql://user:pass@host:port/db");
        }

        String username = matcher.group(1);
        String password = matcher.group(2);
        String host = matcher.group(3);
        int port = Integer.parseInt(matcher.group(4));
        String database = matcher.group(5);

        Map<String, Object> map = new HashMap<>();
        map.put("spring.datasource.url", "jdbc:postgresql://" + host + ":" + port + "/" + database);
        map.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        map.put("spring.datasource.username", username);
        map.put("spring.datasource.password", password);
        environment.getPropertySources().addFirst(new MapPropertySource("railway-database-config", map));
    }
}
