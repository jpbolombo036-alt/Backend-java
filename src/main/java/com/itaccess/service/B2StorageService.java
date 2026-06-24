package com.itaccess.service;

import com.itaccess.config.B2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class B2StorageService {

    private final B2Properties b2Properties;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (!b2Properties.isEnabled()) {
            log.info("B2 storage is disabled");
            return;
        }
        try {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                    b2Properties.getKeyId(),
                    b2Properties.getApplicationKey()
            );
            this.s3Client = S3Client.builder()
                    .endpointOverride(URI.create(b2Properties.getEndpoint()))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .build();
            log.info("B2 S3 client initialized for bucket: {}", b2Properties.getBucket());
        } catch (Exception e) {
            log.error("Failed to initialize B2 S3 client", e);
            throw new IllegalStateException("Unable to initialize B2 storage", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    public String upload(MultipartFile file, String objectKey, String contentType) throws IOException {
        if (!b2Properties.isEnabled()) {
            throw new IllegalStateException("B2 storage is not enabled");
        }
        long start = System.currentTimeMillis();
        try (InputStream is = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(b2Properties.getBucket())
                    .key(objectKey)
                    .contentType(contentType != null ? contentType : file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(is, file.getSize()));
        }
        long duration = System.currentTimeMillis() - start;
        log.info("Uploaded {} to B2 ({} bytes) in {}ms", objectKey, file.getSize(), duration);
        return objectKey;
    }

    public String upload(Path filePath, String objectKey, String contentType, long size) throws IOException {
        if (!b2Properties.isEnabled()) {
            throw new IllegalStateException("B2 storage is not enabled");
        }
        long start = System.currentTimeMillis();
        try (InputStream is = java.nio.file.Files.newInputStream(filePath)) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(b2Properties.getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(size)
                    .build();
            s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(is, size));
        }
        long duration = System.currentTimeMillis() - start;
        log.info("Uploaded {} to B2 ({} bytes) in {}ms", objectKey, size, duration);
        return objectKey;
    }

    public boolean exists(String objectKey) {
        if (!b2Properties.isEnabled()) {
            return false;
        }
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(b2Properties.getBucket())
                    .key(objectKey)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.warn("Error checking B2 object existence for key {}: {}", objectKey, e.getMessage());
            return false;
        }
    }

    public long getSize(String objectKey) {
        if (!b2Properties.isEnabled()) {
            return -1;
        }
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(b2Properties.getBucket())
                    .key(objectKey)
                    .build();
            return s3Client.headObject(headRequest).contentLength();
        } catch (Exception e) {
            log.warn("Error getting B2 object size for key {}: {}", objectKey, e.getMessage());
            return -1;
        }
    }

    public Resource downloadAsResource(String objectKey, String originalFileName, String contentType) throws IOException {
        if (!b2Properties.isEnabled()) {
            throw new IllegalStateException("B2 storage is not enabled");
        }
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(b2Properties.getBucket())
                .key(objectKey)
                .build();
        ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectResponse> s3Object = s3Client.getObject(getRequest);
        String downloadFileName = originalFileName != null ? originalFileName : objectKey.substring(objectKey.lastIndexOf('/') + 1);
        return new B2Resource(s3Object, contentType, downloadFileName, s3Object.response());
    }

    public void delete(String objectKey) {
        if (!b2Properties.isEnabled()) {
            return;
        }
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(b2Properties.getBucket())
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Deleted object from B2: {}", objectKey);
        } catch (Exception e) {
            log.warn("Failed to delete object from B2: {} - {}", objectKey, e.getMessage());
        }
    }

    public String buildObjectKey(String uniqueFileName) {
        return b2Properties.getDocumentsPrefix() + uniqueFileName;
    }

    public static class B2Resource extends InputStreamResource {
        private final String contentType;
        private final String filename;
        private final long contentLength;

        public B2Resource(ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectResponse> delegate, String contentType, String filename,
                          software.amazon.awssdk.services.s3.model.GetObjectResponse response) {
            super(delegate);
            this.contentType = contentType;
            this.filename = filename;
            this.contentLength = response.contentLength();
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        public String getContentType() {
            return contentType != null ? contentType : org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}
