package com.finanzapp.lifehub.modules.home.controller;

import com.finanzapp.lifehub.modules.home.api.HomeDtos.*;
import com.finanzapp.lifehub.modules.home.domain.AttachmentType;
import com.finanzapp.lifehub.modules.home.domain.HomeItemStatus;
import com.finanzapp.lifehub.modules.home.service.HomeService;
import com.finanzapp.lifehub.modules.home.service.HomeStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {
    private final HomeService service;
    private final HomeStorageService storage;

    @GetMapping("/categories")
    public List<CategoryResponse> categories(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(defaultValue = "personal") String scope) {
        return service.categories(owner(jwt), scope);
    }

    @PostMapping("/categories")
    public CategoryResponse createCategory(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CategoryRequest request) {
        return service.createCategory(owner(jwt), request);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.deleteCategory(owner(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items")
    public List<ItemResponse> items(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(defaultValue = "personal") String scope,
                                    @RequestParam(defaultValue = "") String query,
                                    @RequestParam(required = false) Long categoryId,
                                    @RequestParam(required = false) HomeItemStatus status) {
        return service.search(owner(jwt), scope, query, categoryId, status);
    }

    @GetMapping("/items/archived")
    public List<ItemResponse> archived(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(defaultValue = "personal") String scope) {
        return service.archived(owner(jwt), scope);
    }

    @GetMapping("/items/warranty/expiring")
    public List<ItemResponse> expiring(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(defaultValue = "personal") String scope,
                                       @RequestParam(defaultValue = "30") int days) {
        return service.expiring(owner(jwt), scope, days);
    }

    @GetMapping("/items/{id}")
    public ItemResponse item(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getItem(owner(jwt), id);
    }

    @PostMapping(value = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ItemResponse createItem(@AuthenticationPrincipal Jwt jwt,
                                   @Valid @RequestPart("item") ItemRequest request,
                                   @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                   @RequestParam(value = "fileTypes", required = false) List<AttachmentType> fileTypes) {
        return service.createItem(owner(jwt), request, files, fileTypes);
    }

    @PutMapping("/items/{id}")
    public ItemResponse updateItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                   @Valid @RequestBody ItemRequest request) {
        return service.updateItem(owner(jwt), id, request);
    }

    @PatchMapping("/items/{id}/sharing")
    public ItemResponse updateSharing(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable Long id,
                                      @RequestParam boolean shared) {
        return service.updateSharing(owner(jwt), id, shared);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> archive(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.archive(owner(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/items/{id}/restore")
    public ItemResponse restore(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.restore(owner(jwt), id);
    }

    @DeleteMapping("/items/{id}/permanent")
    public ResponseEntity<Void> deletePermanently(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.deletePermanently(owner(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/items/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentResponse attachment(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                         @RequestPart("file") MultipartFile file,
                                         @RequestParam(defaultValue = "OTHER") AttachmentType type) {
        return service.addAttachment(owner(jwt), id, file, type);
    }

    @GetMapping("/items/{itemId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal Jwt jwt, @PathVariable Long itemId,
                                             @PathVariable Long attachmentId) {
        var attachment = service.attachmentEntity(owner(jwt), itemId, attachmentId);
        Resource resource = storage.load(attachment.getStorageKey());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getOriginalFilename().replace("\"", "") + "\"")
                .body(resource);
    }

    private String owner(Jwt jwt) {
        String email = jwt == null ? null : jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("El token no contiene email");
        return email;
    }
}
