package com.itaccess.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.storage.b2")
public class B2Properties {
    private boolean enabled = false;
    private String keyId;
    private String applicationKey;
    private String bucket = "taccess-storage";
    private String endpoint;
    private String region = "us-west-002";
    private String documentsPrefix = "document-archive/";
}
