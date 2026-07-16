package com.itaccess.service;

import com.itaccess.config.ApplicationContextProvider;
import com.itaccess.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {

    private final S3Properties s3Properties;
    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (!s3Properties.isEnabled()) {
            log.info("S3 storage is disabled");
            return;
        }
        this.s3Client = ApplicationContextProvider.getBean(S3Client.class);
        log.info("S3 storage service initialized for bucket: {}", s3Properties.getBucket());
    }

    @PreDestroy
    public void cleanup() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    public String upload(MultipartFile file, String objectKey, String contentType) throws IOException {
        if (!s3Properties.isEnabled() || s3Client == null) {
            throw new IllegalStateException("S3 storage is not enabled");
        }
        long start = System.currentTimeMillis();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(objectKey)
                .contentType(contentType != null ? contentType : file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        long duration = System.currentTimeMillis() - start;
        log.info("Uploaded {} to S3 ({} bytes) in {}ms", objectKey, file.getSize(), duration);
        return objectKey;
    }

    public String upload(Path filePath, String objectKey, String contentType, long size) throws IOException {
        if (!s3Properties.isEnabled() || s3Client == null) {
            throw new IllegalStateException("S3 storage is not enabled");
        }
        long start = System.currentTimeMillis();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .contentLength(size)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(filePath));

        long duration = System.currentTimeMillis() - start;
        log.info("Uploaded {} to S3 ({} bytes) in {}ms", objectKey, size, duration);
        return objectKey;
    }

    public boolean exists(String objectKey) {
        if (!s3Properties.isEnabled() || s3Client == null) {
            return false;
        }
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .build();
            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            return response != null;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.warn("Error checking S3 object existence for key {}: {}", objectKey, e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Error checking S3 object existence for key {}: {}", objectKey, e.getMessage());
            return false;
        }
    }

    public long getSize(String objectKey) {
        if (!s3Properties.isEnabled() || s3Client == null) {
            return -1;
        }
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .build();
            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            return response.contentLength();
        } catch (Exception e) {
            log.warn("Error getting S3 object size for key {}: {}", objectKey, e.getMessage());
            return -1;
        }
    }

    public Resource downloadAsResource(String objectKey, String originalFileName, String contentType) throws IOException {
        if (!s3Properties.isEnabled() || s3Client == null) {
            throw new IllegalStateException("S3 storage is not enabled");
        }

        try {
            software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .build();

            ResponseInputStream<?> s3Object = s3Client.getObject(getObjectRequest);
            byte[] body = s3Object.readAllBytes();

            return new S3Resource(body, contentType, originalFileName != null ? originalFileName : objectKey);
        } catch (Exception e) {
            throw new IOException("Failed to download from S3: " + e.getMessage(), e);
        }
    }

    public void delete(String objectKey) {
        if (!s3Properties.isEnabled() || s3Client == null) {
            return;
        }
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Deleted object from S3: {}", objectKey);
        } catch (Exception e) {
            log.warn("Failed to delete object from S3: {} - {}", objectKey, e.getMessage());
        }
    }

    public String buildObjectKey(String uniqueFileName) {
        return s3Properties.getDocumentsPrefix() + uniqueFileName;
    }

    public static class S3Resource extends InputStreamResource {
        private final String contentType;
        private final String filename;
        private final byte[] data;

        public S3Resource(byte[] data, String contentType, String filename) {
            super(new ByteArrayInputStream(data));
            this.contentType = contentType;
            this.filename = filename;
            this.data = data;
        }

        @Override
        public long contentLength() {
            return data.length;
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
