package com.itaccess.controller;

import com.itaccess.entity.Bug;
import com.itaccess.service.BugService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bugs")
@RequiredArgsConstructor
public class BugController {

    private final BugService bugService;

    @PostMapping
    public ResponseEntity<Bug> createBug(@RequestBody Bug bug, @RequestHeader("X-User-Id") Long userId) {
        return new ResponseEntity<>(bugService.createBug(bug, userId), HttpStatus.CREATED);
    }

    @GetMapping("/step/{testStepId}")
    public ResponseEntity<List<Bug>> getBugsByStep(@PathVariable Long testStepId) {
        return ResponseEntity.of(java.util.Optional.ofNullable(bugService.getBugsByStep(testStepId)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Bug> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(bugService.updateStatus(id, status));
    }
}