package com.cms.controller;

import com.cms.model.Paper;
import com.cms.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequiredArgsConstructor
public class FileController {

    private final PaperService paperService;

    @GetMapping("/papers/view/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER', 'AUTHOR')")
    public ResponseEntity<Resource> viewFileInline(@PathVariable Long id) {
        try {
            Paper paper = paperService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Paper not found with ID: " + id));

            // Ensure this matches the 'uploadDir' in your PaperServiceImpl
            Path path = Paths.get("uploads").resolve(paper.getFilePath());
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + paper.getFileName() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read file: " + paper.getFileName());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
}