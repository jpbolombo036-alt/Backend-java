package com.itaccess.controller;

import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
public class DebugController {

    private static final String EXPECTED_TOKEN = "debug-access";

    private final Environment environment;

    public DebugController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/debug/config")
    public ResponseEntity<?> debugConfig(@RequestHeader(value = "X-Debug-Token", required = false) String token) {
        if (!Objects.equals(EXPECTED_TOKEN, token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("datasourceUrl", maskPassword(environment.getProperty("spring.datasource.url")));
        result.put("datasourceUsername", environment.getProperty("spring.datasource.username"));
        result.put("datasourceDriver", environment.getProperty("spring.datasource.driver-class-name"));
        result.put("jwtSecret", maskJwt(environment.getProperty("app.jwt.secret")));
        result.put("serverPort", environment.getProperty("server.port"));
        result.put("activeProfiles", String.join(",", environment.getActiveProfiles()));
        result.put("railwayEnvPresent", environment.getProperty("DATABASE_URL") != null);
        result.put("springDatasourceUrlPresent", environment.getProperty("SPRING_DATASOURCE_URL") != null);
        result.put("s3Enabled", environment.getProperty("app.storage.s3.enabled"));
        result.put("s3Bucket", environment.getProperty("app.storage.s3.bucket"));
        result.put("s3Endpoint", environment.getProperty("app.storage.s3.endpoint"));
        result.put("s3Region", environment.getProperty("app.storage.s3.region"));
        result.put("s3DocumentsPrefix", environment.getProperty("app.storage.s3.documents-prefix"));
        result.put("s3ApkPrefix", environment.getProperty("app.storage.s3.apk-prefix"));
        result.put("s3AccessKeyPresent", environment.getProperty("app.storage.s3.access-key") != null);
        result.put("s3SecretKeyPresent", environment.getProperty("app.storage.s3.secret-key") != null);
        return ResponseEntity.ok(result);
    }

    private String maskPassword(String url) {
        if (url == null) return null;
        return url.replaceAll(":[^:@/]+@", ":***@");
    }

    private String maskJwt(String jwt) {
        if (jwt == null || jwt.length() <= 12) return jwt;
        return jwt.substring(0, 6) + "..." + jwt.substring(jwt.length() - 4);
    }
}
