package com.itaccess.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class S3Config {

    private final S3Properties s3Properties;

    private Region resolveRegion() {
        String region = s3Properties.getRegion();
        if (region == null || region.isBlank() || "auto".equalsIgnoreCase(region)) {
            return Region.of("us-east-1");
        }
        return Region.of(region);
    }

    @Bean
    public S3Client s3Client() {
        if (!s3Properties.isEnabled()) {
            log.info("S3 storage is disabled");
            return null;
        }

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                s3Properties.getAccessKey(),
                s3Properties.getSecretKey()
        );

        Region region = resolveRegion();

        S3ClientBuilder builder = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());

        if (s3Properties.getEndpoint() != null && !s3Properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3Properties.getEndpoint()));
        }

        S3Client client = builder.build();
        log.info("S3 client initialized for bucket: {} at endpoint: {}", s3Properties.getBucket(), s3Properties.getEndpoint());
        return client;
    }

    @Bean
    public S3Presigner s3Presigner(S3Client s3Client) {
        if (!s3Properties.isEnabled() || s3Client == null) {
            return null;
        }
        Region region = resolveRegion();
        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3Properties.getAccessKey(), s3Properties.getSecretKey())
                ))
                .endpointOverride(URI.create(s3Properties.getEndpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .s3Client(s3Client)
                .build();
    }
}
