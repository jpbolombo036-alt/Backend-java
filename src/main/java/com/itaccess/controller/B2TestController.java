package com.itaccess.controller;

import com.itaccess.service.B2StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class B2TestController {

    private final B2StorageService b2StorageService;

    @PostMapping("/b2/upload")
    public ResponseEntity<String> testUpload(@RequestParam MultipartFile file) throws IOException {
        String key = b2StorageService.buildObjectKey("test/" + file.getOriginalFilename());
        b2StorageService.upload(file, key, file.getContentType());
        return ResponseEntity.ok("Uploaded to B2: " + key);
    }

    @GetMapping("/b2/exists")
    public ResponseEntity<Boolean> testExists(@RequestParam String key) {
        return ResponseEntity.ok(b2StorageService.exists(key));
    }

    @DeleteMapping("/b2/delete")
    public ResponseEntity<String> testDelete(@RequestParam String key) {
        b2StorageService.delete(key);
        return ResponseEntity.ok("Deleted from B2: " + key);
    }
}
