package com.example.endtrem.controller;

import com.example.endtrem.dto.ApiResponse;
import com.example.endtrem.model.PromptTemplate;
import com.example.endtrem.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/prompt-templates")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateRepository promptTemplateRepository;

    /**
     * Get all prompt templates (admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PromptTemplate>>> getAllTemplates() {
        try {
            List<PromptTemplate> templates = promptTemplateRepository.findAll();
            return ResponseEntity.ok(ApiResponse.success(templates));
        } catch (Exception e) {
            log.error("Failed to get templates: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get template by ID
     */
    @GetMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromptTemplate>> getTemplateById(
            @PathVariable String templateId) {
        try {
            PromptTemplate template = promptTemplateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("Template not found"));
            return ResponseEntity.ok(ApiResponse.success(template));
        } catch (Exception e) {
            log.error("Failed to get template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get template by name
     */
    @GetMapping("/by-name/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromptTemplate>> getTemplateByName(
            @PathVariable String name) {
        try {
            PromptTemplate template = promptTemplateRepository.findByName(name)
                    .orElseThrow(() -> new RuntimeException("Template not found"));
            return ResponseEntity.ok(ApiResponse.success(template));
        } catch (Exception e) {
            log.error("Failed to get template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get active template by type
     */
    @GetMapping("/by-type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromptTemplate>> getActiveTemplateByType(
            @PathVariable PromptTemplate.PromptType type) {
        try {
            PromptTemplate template = promptTemplateRepository.findByTypeAndActive(type, true)
                    .orElseThrow(() -> new RuntimeException("Active template not found for type: " + type));
            return ResponseEntity.ok(ApiResponse.success(template));
        } catch (Exception e) {
            log.error("Failed to get template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Create new prompt template
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromptTemplate>> createTemplate(
            @RequestBody CreateTemplateRequest request) {
        try {
            PromptTemplate template = PromptTemplate.builder()
                    .name(request.name())
                    .description(request.description())
                    .template(request.template())
                    .type(request.type())
                    .active(request.active())
                    .version(1)
                    .build();
            
            template = promptTemplateRepository.save(template);
            log.info("Template created: {}", template.getName());
            return ResponseEntity.ok(ApiResponse.success("Template created successfully", template));
        } catch (Exception e) {
            log.error("Failed to create template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update prompt template
     */
    @PutMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PromptTemplate>> updateTemplate(
            @PathVariable String templateId,
            @RequestBody UpdateTemplateRequest request) {
        try {
            PromptTemplate template = promptTemplateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("Template not found"));
            
            if (request.name() != null) template.setName(request.name());
            if (request.description() != null) template.setDescription(request.description());
            if (request.template() != null) template.setTemplate(request.template());
            if (request.type() != null) template.setType(request.type());
            if (request.active() != null) template.setActive(request.active());
            template.setVersion(template.getVersion() + 1);
            
            template = promptTemplateRepository.save(template);
            log.info("Template updated: {}", template.getName());
            return ResponseEntity.ok(ApiResponse.success("Template updated successfully", template));
        } catch (Exception e) {
            log.error("Failed to update template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete prompt template
     */
    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable String templateId) {
        try {
            if (!promptTemplateRepository.existsById(templateId)) {
                throw new RuntimeException("Template not found");
            }
            promptTemplateRepository.deleteById(templateId);
            log.info("Template deleted: {}", templateId);
            return ResponseEntity.ok(ApiResponse.success("Template deleted successfully", null));
        } catch (Exception e) {
            log.error("Failed to delete template: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // Request DTOs
    public record CreateTemplateRequest(
            String name,
            String description,
            String template,
            PromptTemplate.PromptType type,
            boolean active
    ) {}

    public record UpdateTemplateRequest(
            String name,
            String description,
            String template,
            PromptTemplate.PromptType type,
            Boolean active
    ) {}
}
