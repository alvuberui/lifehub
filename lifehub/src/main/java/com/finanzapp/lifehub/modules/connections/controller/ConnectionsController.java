package com.finanzapp.lifehub.modules.connections.controller;

import com.finanzapp.lifehub.modules.connections.api.ConnectionDtos;
import com.finanzapp.lifehub.modules.connections.service.ConnectionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/connections")
@RequiredArgsConstructor
public class ConnectionsController {

    private final ConnectionsService connectionsService;

    @GetMapping
    public ResponseEntity<ConnectionDtos.ConnectionPage> overview(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(connectionsService.overview(email(jwt)));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@AuthenticationPrincipal Jwt jwt, @RequestParam String query) {
        return ResponseEntity.ok(connectionsService.search(email(jwt), query));
    }

    @PostMapping("/requests")
    public ResponseEntity<ConnectionDtos.ConnectionRequestResponse> sendRequest(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ConnectionDtos.SendRequest request) {
        if (request == null || request.recipientEmail() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(connectionsService.sendRequest(email(jwt), request.recipientEmail()));
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<Void> accept(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId) {
        connectionsService.acceptRequest(email(jwt), requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<Void> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId) {
        connectionsService.rejectRequest(email(jwt), requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}/cancel")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long requestId) {
        connectionsService.cancelRequest(email(jwt), requestId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{connectionId}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long connectionId) {
        connectionsService.removeConnection(email(jwt), connectionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/blocks")
    public ResponseEntity<Void> block(@AuthenticationPrincipal Jwt jwt, @RequestBody ConnectionDtos.BlockRequest request) {
        if (request == null || request.email() == null) return ResponseEntity.badRequest().build();
        connectionsService.block(email(jwt), request.email());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{connectionId}/permissions")
    public ResponseEntity<ConnectionDtos.PermissionResponse> updatePermission(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long connectionId,
            @RequestBody ConnectionDtos.PermissionUpdate request) {
        if (request == null || request.module() == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(connectionsService.updatePermission(email(jwt), connectionId, request.module(), request.permission()));
    }

    @GetMapping("/spaces")
    public ResponseEntity<?> spaces(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(connectionsService.listSpaces(email(jwt)));
    }

    @PostMapping("/spaces")
    public ResponseEntity<ConnectionDtos.SharedSpaceResponse> createSpace(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ConnectionDtos.CreateSharedSpace request) {
        if (request == null || request.name() == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.status(HttpStatus.CREATED).body(connectionsService.createSpace(email(jwt), request.name(), request.description()));
    }

    @PostMapping("/spaces/{spaceId}/members")
    public ResponseEntity<Void> addMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long spaceId,
            @RequestBody ConnectionDtos.AddSharedSpaceMember request) {
        if (request == null || request.email() == null) return ResponseEntity.badRequest().build();
        connectionsService.addSpaceMember(email(jwt), spaceId, request.email(), request.role());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/spaces/{spaceId}/members/{memberEmail}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal Jwt jwt, @PathVariable Long spaceId, @PathVariable String memberEmail) {
        connectionsService.removeSpaceMember(email(jwt), spaceId, memberEmail);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/spaces/{spaceId}")
    public ResponseEntity<Void> deleteSpace(@AuthenticationPrincipal Jwt jwt, @PathVariable Long spaceId) {
        connectionsService.deleteSpace(email(jwt), spaceId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/spaces/{spaceId}/permissions")
    public ResponseEntity<Void> updateSpacePermission(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long spaceId,
            @RequestBody ConnectionDtos.SharedSpacePermissionUpdate request) {
        if (request == null || request.module() == null) return ResponseEntity.badRequest().build();
        connectionsService.updateSpacePermission(email(jwt), spaceId, request.module(), request.permission());
        return ResponseEntity.noContent().build();
    }

    private String email(Jwt jwt) {
        if (jwt == null || jwt.getClaimAsString("email") == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado.");
        }
        return jwt.getClaimAsString("email");
    }
}
