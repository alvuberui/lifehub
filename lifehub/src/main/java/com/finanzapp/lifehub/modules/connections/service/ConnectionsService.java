package com.finanzapp.lifehub.modules.connections.service;

import com.finanzapp.lifehub.modules.connections.api.ConnectionDtos;
import com.finanzapp.lifehub.modules.connections.domain.ModulePermission;

import java.util.List;

public interface ConnectionsService {
    List<ConnectionDtos.UserSearchResponse> search(String actorEmail, String query);

    ConnectionDtos.ConnectionPage overview(String actorEmail);

    ConnectionDtos.ConnectionRequestResponse sendRequest(String actorEmail, String recipientEmail);

    void acceptRequest(String actorEmail, Long requestId);

    void rejectRequest(String actorEmail, Long requestId);

    void cancelRequest(String actorEmail, Long requestId);

    void removeConnection(String actorEmail, Long connectionId);

    void block(String actorEmail, String blockedEmail);

    ConnectionDtos.PermissionResponse updatePermission(String actorEmail, Long connectionId, String module, ModulePermission permission);

    boolean hasPermission(String ownerEmail, String viewerEmail, String module, ModulePermission required);

    List<String> usersSharingModule(String viewerEmail, String module, ModulePermission required);

    List<ConnectionDtos.SharedSpaceResponse> listSpaces(String actorEmail);

    ConnectionDtos.SharedSpaceResponse createSpace(String actorEmail, String name, String description);

    void addSpaceMember(String actorEmail, Long spaceId, String memberEmail, String role);

    void removeSpaceMember(String actorEmail, Long spaceId, String memberEmail);

    void deleteSpace(String actorEmail, Long spaceId);

    void updateSpacePermission(String actorEmail, Long spaceId, String module, ModulePermission permission);
}
