package com.finanzapp.lifehub.modules.connections.api;

import com.finanzapp.lifehub.modules.connections.domain.ConnectionRequestStatus;
import com.finanzapp.lifehub.modules.connections.domain.ModulePermission;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class ConnectionDtos {
    private ConnectionDtos() {
    }

    public record UserSearchResponse(
            String email,
            String name,
            String lastname,
            boolean connected,
            boolean requestPending) {
    }

    public record ConnectionResponse(
            Long id,
            String email,
            String name,
            String lastname,
            OffsetDateTime connectedAt,
            Map<String, ModulePermission> grantedByMe,
            Map<String, ModulePermission> grantedToMe) {
    }

    public record ConnectionRequestResponse(
            Long id,
            String requesterEmail,
            String requesterName,
            String requesterLastname,
            String recipientEmail,
            String recipientName,
            String recipientLastname,
            ConnectionRequestStatus status,
            OffsetDateTime createdAt,
            boolean incoming) {
    }

    public record SendRequest(String recipientEmail) {
    }

    public record BlockRequest(String email) {
    }

    public record PermissionUpdate(String module, ModulePermission permission) {
    }

    public record PermissionResponse(String module, ModulePermission permission, String grantedByEmail) {
    }

    public record ConnectionPage(
            List<ConnectionResponse> connections,
            List<ConnectionRequestResponse> pendingRequests) {
    }

    public record SharedSpaceResponse(
            Long id,
            String name,
            String description,
            String ownerEmail,
            List<SharedSpaceMemberResponse> members,
            Map<String, ModulePermission> permissions) {
    }

    public record SharedSpaceMemberResponse(String email, String role) {
    }

    public record CreateSharedSpace(String name, String description) {
    }

    public record AddSharedSpaceMember(String email, String role) {
    }

    public record SharedSpacePermissionUpdate(String module, ModulePermission permission) {
    }
}
