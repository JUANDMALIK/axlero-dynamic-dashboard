package com.axlero.dashboard.controller;

import com.axlero.dashboard.service.DatasetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        if (file == null || file.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choose a CSV file to upload.");
        }

        return datasetService.upload(
                authentication.getName(),
                file.getOriginalFilename(),
                file.getBytes());
    }

    @GetMapping("/latest")
    public Map<String, Object> latest(Authentication authentication) {
        return datasetService.latest(authentication.getName());
    }

    @GetMapping("/latest/analytics")
    public Map<String, Object> analytics(Authentication authentication) {
        return datasetService.analytics(authentication.getName());
    }
}