package com.finanzapp.tags.controller;



import com.finanzapp.tags.controller.dto.CreateTagRequestDto;
import com.finanzapp.tags.controller.dto.TagDto;
import com.finanzapp.tags.controller.mapper.TagMapper;
import com.finanzapp.tags.service.TagService;
import lombok.Builder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;


@RestController
@RequestMapping("/tags")
@Validated
@Builder
public class TagController {

    private final TagService tagService;

    private final TagMapper tagMapper;


    @PostMapping()
    public ResponseEntity<TagDto> createMovement(@Valid @RequestBody CreateTagRequestDto createTagRequestDto, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        TagDto response = tagMapper.toTagDto(
                tagService.createTag( tagMapper.toCreateTagRequest(createTagRequestDto), email)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping()
    public ResponseEntity<List<TagDto>> getTagsByUserEmail(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        List<TagDto> response = tagService.getTagsByUserEmail(email).stream()
                .map(tagMapper::toTagDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tagId}")
    public ResponseEntity<TagDto> getTagByIdAndUserEmail(@PathVariable Long tagId, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        TagDto response = tagMapper.toTagDto(
                tagService.getTagByIdAndUserEmail(tagId, email)
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long tagId, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        tagService.deleteTag(tagId, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists/{tagId}")
    public ResponseEntity<Boolean> existsByIdAndUserEmail(@PathVariable Long tagId, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        Boolean response = tagService.existsByIdAndUserEmail(tagId, email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<TagDto>> getTagsByIdsAndUserEmail(
            @RequestBody @Size(max = 200, message = "No se pueden solicitar más de 200 etiquetas") List<Long> tagIds,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String email = jwt.getClaimAsString("email");

        List<TagDto> response = tagService.getTagsByIdsAndUserEmail(tagIds, email).stream()
                .map(tagMapper::toTagDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/shared/{ownerEmail}")
    public ResponseEntity<List<TagDto>> getSharedTags(
            @PathVariable String ownerEmail,
            @AuthenticationPrincipal Jwt jwt) {
        String actorEmail = jwt.getClaimAsString("email");
        return ResponseEntity.ok(tagService.getTagsForOwner(ownerEmail, actorEmail).stream().map(tagMapper::toTagDto).toList());
    }

    @PostMapping("/shared/{ownerEmail}")
    public ResponseEntity<TagDto> createSharedTag(
            @PathVariable String ownerEmail,
            @Valid @RequestBody CreateTagRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        String actorEmail = jwt.getClaimAsString("email");
        return ResponseEntity.ok(tagMapper.toTagDto(tagService.createTagForOwner(tagMapper.toCreateTagRequest(request), ownerEmail, actorEmail)));
    }

    @DeleteMapping("/shared/{ownerEmail}/{tagId}")
    public ResponseEntity<Void> deleteSharedTag(
            @PathVariable String ownerEmail,
            @PathVariable Long tagId,
            @AuthenticationPrincipal Jwt jwt) {
        tagService.deleteTagForOwner(tagId, ownerEmail, jwt.getClaimAsString("email"));
        return ResponseEntity.noContent().build();
    }

}
