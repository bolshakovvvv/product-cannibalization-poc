package com.cannibalization.back.audience;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audiences")
@RequiredArgsConstructor
public class AudienceController {

    private final AudienceService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AudienceUploadResponse upload(
            @RequestParam("file") MultipartFile file
    ) {
        return service.upload(file);
    }

    @GetMapping("/{id}/members")
    public List<Map<String, Object>> getMembers(@PathVariable UUID id) {
        return service.getMembers(id);
    }


    @PostMapping("/{id}/analyze")
    public Map<String, Object> analyze(@PathVariable UUID id) {
        return service.analyze(id);
    }
}