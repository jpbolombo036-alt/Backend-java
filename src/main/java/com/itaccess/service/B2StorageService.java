package com.itaccess.service;

import com.itaccess.config.B2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class B2StorageService {

    private final B2Properties b2Properties;

    private RestTemplate restTemplate;
    private String downloadUrl;
    private String uploadUrl;
    private String apiUrl;
    private String bucketId;

    @PostConstruct
    public void init() {
        if (!b2Properties.isEnabled()) {
            log.info("B2 storage is disabled");
            return;
        }
        this.restTemplate = new RestTemplate();
        log.info("B2 storage service initialized for bucket: {}", b2Properties.getBucket());
    }

    @PreDestroy
    public void cleanup() {
    }

    private String authorize() {
        String authUrl = "https://api.backblazeb2.com/b2api/v2/b2_authorize_account";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(b2Properties.getKeyId(), b2Properties.getApplicationKey());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    authUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("authorizationToken")) {
                throw new IllegalStateException("Invalid B2 authorization response: missing authorizationToken");
            }

            String authToken = (String) body.get("authorizationToken");
            this.apiUrl = (String) body.get("apiUrl");
            this.downloadUrl = (String) body.get("downloadUrl");

            log.info("B2 authorized: apiUrl={}, downloadUrl={}", apiUrl, downloadUrl);
            return authToken;
        } catch (Exception e) {
            log.error("B2 authorization failed", e);
            throw new IllegalStateException("Unable to authorize B2", e);
        }
    }

    private String resolveBucketId(String authToken) {
        if (this.bucketId != null) {
            return this.bucketId;
        }

        String url = apiUrl + "/b2api/v2/b2_list_buckets";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new HashMap<>();
        body.put("accountId", b2Properties.getKeyId());

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map<String, Object> respBody = response.getBody();
        if (respBody == null || !respBody.containsKey("buckets")) {
            throw new IllegalStateException("Unable to list B2 buckets");
        }

        List<Map<String, Object>> buckets = (List<Map<String, Object>>) respBody.get("buckets");
        log.info("B2 buckets visible: {}", buckets);
        for (Map<String, Object> bucket : buckets) {
            if (b2Properties.getBucket().equals(bucket.get("bucketName"))) {
                this.bucketId = (String) bucket.get("bucketId");
                log.info("B2 bucketId resolved: {} -> {}", b2Properties.getBucket(), this.bucketId);
                return this.bucketId;
            }
        }

        throw new IllegalStateException("Bucket not found: " + b2Properties.getBucket() + " (visible: " + buckets + ")");
    }

    public String upload(MultipartFile file, String objectKey, String contentType) throws IOException {
        if (!b2Properties.isEnabled()) {
            throw new IllegalStateException("B2 storage is not enabled");
        }
        long start = System.currentTimeMillis();

        String authToken = authorize();
        String bucketId = resolveBucketId(authToken);

        HttpHeaders urlHeaders = new HttpHeaders();
        urlHeaders.set("Authorization", authToken);
        urlHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> urlBody = new HashMap<>();
        urlBody.put("bucketId", bucketId);

        ResponseEntity<Map> urlResponse = restTemplate.exchange(
                apiUrl + "/b2api/v2/b2_get_upload_url",
                HttpMethod.POST,
                new HttpEntity<>(urlBody, urlHeaders),
                Map.class
        );
        Map<String, Object> urlRespBody = urlResponse.getBody();
        if (urlRespBody == null || !urlRespBody.containsKey("uploadUrl")) {
            throw new IOException("B2 upload URL request failed: " + urlRespBody);
        }
        String uploadUrl = (String) urlRespBody.get("uploadUrl");
        String uploadAuthToken = (String) urlRespBody.get("authorizationToken");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", uploadAuthToken);
        headers.set("X-Bz-File-Name", objectKey);
        headers.set("X-Bz-Content-Sha1", "do_not_verify");
        headers.set("Content-Type", contentType != null ? contentType : file.getContentType());
        headers.set("Content-Length", String.valueOf(file.getSize()));

        ResponseEntity<Map> uploadResponse = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                new HttpEntity<>(file.getBytes(), headers),
                Map.class
        );

        Map<String, Object> body = uploadResponse.getBody();
        if (body == null || !"upload".equals(body.get("action"))) {
            throw new IOException("B2 upload failed: " + body);
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

        String authToken = authorize();
        String bucketId = resolveBucketId(authToken);

        HttpHeaders urlHeaders = new HttpHeaders();
        urlHeaders.set("Authorization", authToken);
        urlHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> urlBody = new HashMap<>();
        urlBody.put("bucketId", bucketId);

        ResponseEntity<Map> urlResponse = restTemplate.exchange(
                apiUrl + "/b2api/v2/b2_get_upload_url",
                HttpMethod.POST,
                new HttpEntity<>(urlBody, urlHeaders),
                Map.class
        );
        Map<String, Object> urlRespBody = urlResponse.getBody();
        if (urlRespBody == null || !urlRespBody.containsKey("uploadUrl")) {
            throw new IOException("B2 upload URL request failed: " + urlRespBody);
        }
        String uploadUrl = (String) urlRespBody.get("uploadUrl");
        String uploadAuthToken = (String) urlRespBody.get("authorizationToken");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", uploadAuthToken);
        headers.set("X-Bz-File-Name", objectKey);
        headers.set("X-Bz-Content-Sha1", "do_not_verify");
        headers.set("Content-Type", contentType);
        headers.set("Content-Length", String.valueOf(size));

        byte[] fileBytes = java.nio.file.Files.readAllBytes(filePath);
        ResponseEntity<Map> uploadResponse = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                new HttpEntity<>(fileBytes, headers),
                Map.class
        );

        Map<String, Object> body = uploadResponse.getBody();
        if (body == null || !"upload".equals(body.get("action"))) {
            throw new IOException("B2 upload failed: " + body);
        }

        long duration = System.currentTimeMillis() - start;
        log.info("Uploaded {} to B2 ({} bytes) in {}ms", objectKey, size, duration);
        return objectKey;
    }

    public boolean exists(String objectKey) {
        if (!b2Properties.isEnabled()) {
            return false;
        }
        String authToken = authorize();
        String url = downloadUrl + "/file/" + b2Properties.getBucket() + "/" + objectKey;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.HEAD,
                    new HttpEntity<>(headers),
                    byte[].class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Error checking B2 object existence for key {}: {}", objectKey, e.getMessage());
            return false;
        }
    }

    public long getSize(String objectKey) {
        if (!b2Properties.isEnabled()) {
            return -1;
        }
        String authToken = authorize();
        String url = downloadUrl + "/file/" + b2Properties.getBucket() + "/" + objectKey;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.HEAD,
                    new HttpEntity<>(headers),
                    byte[].class
            );
            String cl = response.getHeaders().getFirst("Content-Length");
            return cl != null ? Long.parseLong(cl) : -1;
        } catch (Exception e) {
            log.warn("Error getting B2 object size for key {}: {}", objectKey, e.getMessage());
            return -1;
        }
    }

    public Resource downloadAsResource(String objectKey, String originalFileName, String contentType) throws IOException {
        if (!b2Properties.isEnabled()) {
            throw new IllegalStateException("B2 storage is not enabled");
        }
        String authToken = authorize();
        String url = downloadUrl + "/file/" + b2Properties.getBucket() + "/" + objectKey;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class
            );
            byte[] body = response.getBody();
            if (body == null) {
                throw new IOException("Empty response from B2");
            }
            return new B2Resource(body, contentType, originalFileName != null ? originalFileName : objectKey);
        } catch (Exception e) {
            throw new IOException("Failed to download from B2: " + e.getMessage(), e);
        }
    }

    public void delete(String objectKey) {
        if (!b2Properties.isEnabled()) {
            return;
        }
        try {
            String authToken = authorize();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("fileName", objectKey);
            body.put("bucketId", b2Properties.getBucket());

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl + "/b2api/v2/b2_delete_file_version",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            log.info("Deleted object from B2: {} (status={})", objectKey, response.getStatusCode());
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
        private final byte[] data;

        public B2Resource(byte[] data, String contentType, String filename) {
            super(new java.io.ByteArrayInputStream(data));
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
            return contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}
