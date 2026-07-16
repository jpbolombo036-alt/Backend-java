package com.itaccess.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.storage.s3")
public class S3Properties {
    private boolean enabled = false;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String endpoint;
    private String region;
    private String documentsPrefix = "document-archive/";
    private String apkPrefix = "apk/";
}
