package com.finanzapp.lifehub.modules.connections.service.impl;

import com.finanzapp.lifehub.modules.connections.api.ConnectionDtos;
import com.finanzapp.lifehub.modules.connections.domain.ConnectionRequestStatus;
import com.finanzapp.lifehub.modules.connections.domain.ModulePermission;
import com.finanzapp.lifehub.modules.connections.repository.BlockedUserRepository;
import com.finanzapp.lifehub.modules.connections.repository.ConnectionRepository;
import com.finanzapp.lifehub.modules.connections.repository.ConnectionRequestRepository;
import com.finanzapp.lifehub.modules.connections.repository.ModulePermissionRepository;
import com.finanzapp.lifehub.modules.connections.repository.SharedSpaceRepository;
import com.finanzapp.lifehub.modules.connections.repository.SharedSpaceMemberRepository;
import com.finanzapp.lifehub.modules.connections.repository.SpaceModulePermissionRepository;
import com.finanzapp.lifehub.modules.connections.repository.entities.BlockedUserEntity;
import com.finanzapp.lifehub.modules.connections.repository.entities.ConnectionEntity;
import com.finanzapp.lifehub.modules.connections.repository.entities.ConnectionRequestEntity;
import com.finanzapp.lifehub.modules.connections.repository.entities.ModulePermissionEntity;
import com.finanzapp.lifehub.modules.connections.repository.entities.SharedSpaceEntity;
import com.finanzapp.lifehub.modules.connections.repository.entities.SharedSpaceMemberEntity;
import com.finanzapp.lifehub.modules.connections.repository.entities.SpaceModulePermissionEntity;
import com.finanzapp.lifehub.modules.connections.service.ConnectionsService;
import com.finanzapp.lifehub.modules.identity.user.repository.UserRepository;
import com.finanzapp.lifehub.modules.identity.user.repository.entities.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ConnectionsServiceImpl implements ConnectionsService {

    private static final Set<String> SUPPORTED_MODULES = Set.of("finance", "home", "travel", "tasks", "vehicle", "shopping", "documents", "ai");

    private final UserRepository userRepository;
    private final ConnectionRequestRepository requestRepository;
    private final ConnectionRepository connectionRepository;
    private final ModulePermissionRepository permissionRepository;
    private final BlockedUserRepository blockedUserRepository;
    private final SharedSpaceRepository sharedSpaceRepository;
    private final SharedSpaceMemberRepository sharedSpaceMemberRepository;
    private final SpaceModulePermissionRepository spacePermissionRepository;

    @Override
    @Transactional(readOnly = true, transactionManager = "connectionsTransactionManager")
    public List<ConnectionDtos.UserSearchResponse> search(String actorEmail, String query) {
        String normalizedActor = normalizeEmail(actorEmail);
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 2) return List.of();

        return userRepository.findTop20ByEmailContainingIgnoreCaseOrNameContainingIgnoreCaseOrLastnameContainingIgnoreCase(
                        normalizedQuery, normalizedQuery, normalizedQuery)
                .stream()
                .filter(user -> !normalizeEmail(user.getEmail()).equals(normalizedActor))
                .filter(user -> !isBlockedEitherWay(normalizedActor, normalizeEmail(user.getEmail())))
                .map(user -> new ConnectionDtos.UserSearchResponse(
                        user.getEmail(), user.getName(), user.getLastname(),
                        findConnection(normalizedActor, normalizeEmail(user.getEmail())).isPresent(),
                        hasPendingRequestEitherWay(normalizedActor, normalizeEmail(user.getEmail()))))
                .toList();
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "connectionsTransactionManager")
    public ConnectionDtos.ConnectionPage overview(String actorEmail) {
        String actor = normalizeEmail(actorEmail);
        List<ConnectionDtos.ConnectionResponse> connections = connectionRepository
                .findByUserAEmailOrUserBEmailOrderByCreatedAtDesc(actor, actor)
                .stream()
                .map(connection -> toConnectionResponse(connection, actor))
                .toList();

        List<ConnectionDtos.ConnectionRequestResponse> pending = requestRepository
                .findByRequesterEmailOrRecipientEmailOrderByCreatedAtDesc(actor, actor)
                .stream()
                .filter(request -> request.getStatus() == ConnectionRequestStatus.PENDING)
                .map(request -> toRequestResponse(request, actor))
                .toList();

        return new ConnectionDtos.ConnectionPage(connections, pending);
    }

    @Override
    @Transactional(transactionManager = "connectionsTransactionManager")
    public ConnectionDtos.ConnectionRequestResponse sendRequest(String actorEmail, String recipientEmail) {
        String requester = normalizeEmail(actorEmail);
        String recipient = normalizeEmail(recipientEmail);
        if (requester.isBlank() || recipient.isBlank() || requester.equals(recipient)) {
            throw badRequest("No puedes enviarte una solicitud a ti mismo.");
        }
        UserEntity recipientUser = userRepository.findByEmail(recipient)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe ningún usuario con ese email."));
        if (isBlockedEitherWay(requester, recipient)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No se puede crear esta conexión.");
        }
        if (findConnection(requester, recipient).isPresent()) {
            throw badRequest("Ya existe una conexión con este usuario.");
        }
        if (hasPendingRequestEitherWay(requester, recipient)) {
            throw badRequest("Ya existe una solicitud pendiente entre estos usuarios.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        ConnectionRequestEntity saved = requestRepository.save(ConnectionRequestEntity.builder()
                .requesterEmail(requester)
                .recipientEmail(recipient)
                .status(ConnectionRequestStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return toRequestResponse(saved, requester, recipientUser);
    }

    @Override
    @Transactional(transactionManager = "connectionsTransactionManager")
    public void acceptRequest(String actorEmail, Long requestId) {
        String actor = normalizeEmail(actorEmail);
        ConnectionRequestEntity request = requestRepository.findById(requestId).orElseThrow(() -> notFound("Solicitud no encontrada."));
        if (!actor.equals(normalizeEmail(request.getRecipientEmail())) || request.getStatus() != ConnectionRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes aceptar esta solicitud.");
        }
        if (isBlockedEitherWay(actor, normalizeEmail(request.getRequesterEmail()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No se puede crear esta conexión.");
        }
        String[] pair = canonicalPair(actor, request.getRequesterEmail());
        connectionRepository.findByUserAEmailAndUserBEmail(pair[0], pair[1]).orElseGet(() -> connectionRepository.save(ConnectionEntity.builder()
                .userAEmail(pair[0])
                .userBEmail(pair[1])
                .createdAt(OffsetDateTime.now())
                .build()));
        request.setStatus(ConnectionRequestStatus.ACCEPTED);
        request.setUpdatedAt(OffsetDateTime.now());
        requestRepository.save(request);
    }

    @Override
    @Transactional(transactionManager = "connectionsTransactionManager")
    public void rejectRequest(String actorEmail, Long requestId) {
        updateRequestStatus(actorEmail, requestId, ConnectionRequestStatus.REJECTED, true);
    }

    @Override
    @Transactional(transactionManager = "connectionsTransactionManager")
    public void cancelRequest(String actorEmail, Long requestId) {
        updateRequestStatus(actorEmail, requestId, ConnectionRequestStatus.CANCELLED, false);
    }

    @Override
    @Transactional(transactionManager = "connectionsTransactionManager")
    public void removeConnection(String actorEmail, Long connectionId) {
        String actor = normalizeEmail(actorEmail);
        ConnectionEntity connection = getConnectionForActor(actor, connectionId);
        connectionRepository.delete(connection);
    }

    @Override
    @Transactional(transactionManager = "connectionsTransactionManager")
    public void block(String actorEmail, String blockedEmail) {
        String actor = normalizeEmail(actorEmail);
        String blocked = normalizeEmail(blockedEmail);
        if (actor.isBlank() || blocked.isBlank() || actor.equals(blocked)) throw badRequest("Usuario bloqueado no válido.");
        if (!blockedUserRepository.existsByBlockerEmailAndBlockedEmail(actor, blocked)) {
            blockedUserRepository.save(BlockedUserEntity.builder().blockerEmail(actor).blockedEmail(blocked).createdAt(OffsetDateTime.now()).build());
        }
        findConnection(actor, blocked).ifPresent(connectionRepository::delete);
    }

    @Override
    @Transactional(transactionManager = "connectionsTransactionManager")
    public ConnectionDtos.PermissionResponse updatePermission(String actorEmail, Long connectionId, String module, ModulePermission permission) {
        String actor = normalizeEmail(actorEmail);
        String moduleKey = normalizeModule(module);
        ConnectionEntity connection = getConnectionForActor(actor, connectionId);
        ModulePermission safePermission = permission == null ? ModulePermission.NONE : permission;
        // Los permisos son direccionales: esta fila representa únicamente lo que
        // el actor comparte con la otra persona. Nunca debe reutilizar la fila
        // que representa la dirección contraria.
        ModulePermissionEntity entity = permissionRepository.findByConnectionIdAndModuleKeyAndGrantedByEmail(connection.getId(), moduleKey, actor)
                .orElseGet(() -> ModulePermissionEntity.builder()
                        .connectionId(connection.getId())
                        .moduleKey(moduleKey)
                        .grantedByEmail(actor)
                        .build());
        entity.setPermission(safePermission);
        entity.setGrantedByEmail(actor);
        entity.setUpdatedAt(OffsetDateTime.now());
        permissionRepository.save(entity);
        return new ConnectionDtos.PermissionResponse(moduleKey, safePermission, actor);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "connectionsTransactionManager")
    public boolean hasPermission(String ownerEmail, String viewerEmail, String module, ModulePermission required) {
        if (normalizeEmail(ownerEmail).equals(normalizeEmail(viewerEmail))) return true;
        if (required == null || required == ModulePermission.NONE) return true;
        Optional<ConnectionEntity> connection = findConnection(ownerEmail, viewerEmail);
        if (connection.isEmpty()) return false;
        return permissionRepository.findByConnectionIdAndModuleKeyAndGrantedByEmail(
                        connection.get().getId(), normalizeModule(module), normalizeEmail(ownerEmail))
                .map(permission -> permissionAllows(permission.getPermission(), required))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "connectionsTransactionManager")
    public List<String> usersSharingModule(String viewerEmail, String module, ModulePermission required) {
        String viewer = normalizeEmail(viewerEmail);
        String moduleKey = normalizeModule(module);
        return connectionRepository.findByUserAEmailOrUserBEmailOrderByCreatedAtDesc(viewer, viewer).stream()
                .map(connection -> viewer.equals(normalizeEmail(connection.getUserAEmail())) ? connection.getUserBEmail() : connection.getUserAEmail())
                .filter(owner -> hasPermission(owner, viewer, moduleKey, required))
                .toList();
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "connectionsTransactionManager")
    public List<ConnectionDtos.SharedSpaceResponse> listSpaces(String actorEmail) {
        return sharedSpaceRepository.findVisibleTo(normalizeEmail(actorEmail)).stream().map(this::toSpaceResponse).toList();
    }

    @Override
    @Transactional(transactionManager = "connectionsTransactionManager")
    public ConnectionDtos.SharedSpaceResponse createSpace(String actorEmail, String name, String description) {
        String owner = normalizeEmail(actorEmail);
        String safeName = name == null ? "" : name.trim();
        if (safeName.isBlank() || safeName.length() > 120) throw badRequest("El nombre del espacio no es válido.");
        OffsetDateTime now = OffsetDateTime.now();
        SharedSpaceEntity space = sharedSpaceRepository.save(SharedSpaceEntity.builder().name(safeName).description(description == null ? null : description.trim()).ownerEmail(owner).createdAt(now).build());
        sharedSpaceMemberRepository.save(SharedSpaceMemberEntity.builder().spaceId(space.getId()).userEmail(owner).role("OWNER").joinedAt(now).build());
        return toSpaceResponse(space);
    }

    @Override
    @Transactional(transactionManager = "connectionsTransactionManager")
    public void addSpaceMember(String actorEmail, Long spaceId, String memberEmail, String role) {
        String actor = normalizeEmail(actorEmail);
        String member = normalizeEmail(memberEmail);
        SharedSpaceEntity space = getSpace(spaceId);
        requireSpaceOwner(actor, space);
        if (userRepository.findByEmail(member).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ese usuario no existe.");
        if (!actor.equals(member) && findConnection(actor, member).isEmpty()) throw badRequest("Solo puedes añadir conexiones aceptadas al espacio.");
        if (sharedSpaceMemberRepository.findBySpaceIdAndUserEmail(spaceId, member).isPresent()) throw badRequest("El usuario ya pertenece al espacio.");
        String safeRole = role == null ? "MEMBER" : role.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ADMIN", "MEMBER").contains(safeRole)) throw badRequest("Rol no válido.");
        sharedSpaceMemberRepository.save(SharedSpaceMemberEntity.builder().spaceId(spaceId).userEmail(member).role(safeRole).joinedAt(OffsetDateTime.now()).build());
    }

    @Override
    @Transactional(transactionManager = "connectionsTransactionManager")
    public void removeSpaceMember(String actorEmail, Long spaceId, String memberEmail) {
        SharedSpaceEntity space = getSpace(spaceId);
        requireSpaceOwner(normalizeEmail(actorEmail), space);
        String member = normalizeEmail(memberEmail);
        if (normalizeEmail(space.getOwnerEmail()).equals(member)) throw badRequest("El propietario no puede abandonar el espacio.");
        sharedSpaceMemberRepository.findBySpaceIdAndUserEmail(spaceId, member).ifPresent(sharedSpaceMemberRepository::delete);
    }

    @Override
    @Transactional(transactionManager = "connectionsTransactionManager")
    public void deleteSpace(String actorEmail, Long spaceId) {
        SharedSpaceEntity space = getSpace(spaceId);
        requireSpaceOwner(normalizeEmail(actorEmail), space);
        sharedSpaceRepository.delete(space);
    }

    @Override
    @Transactional(transactionManager = "connectionsTransactionManager")
    public void updateSpacePermission(String actorEmail, Long spaceId, String module, ModulePermission permission) {
        SharedSpaceEntity space = getSpace(spaceId);
        String actor = normalizeEmail(actorEmail);
        requireSpaceOwner(actor, space);
        String moduleKey = normalizeModule(module);
        SpaceModulePermissionEntity entity = spacePermissionRepository.findBySpaceIdAndModuleKey(spaceId, moduleKey)
                .orElseGet(() -> SpaceModulePermissionEntity.builder().spaceId(spaceId).moduleKey(moduleKey).build());
        entity.setPermission(permission == null ? ModulePermission.NONE : permission);
        entity.setGrantedByEmail(actor);
        entity.setUpdatedAt(OffsetDateTime.now());
        spacePermissionRepository.save(entity);
    }

    private SharedSpaceEntity getSpace(Long spaceId) {
        return sharedSpaceRepository.findById(spaceId).orElseThrow(() -> notFound("Espacio no encontrado."));
    }

    private void requireSpaceOwner(String actor, SharedSpaceEntity space) {
        if (!normalizeEmail(space.getOwnerEmail()).equals(actor)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el propietario puede gestionar este espacio.");
    }

    private ConnectionDtos.SharedSpaceResponse toSpaceResponse(SharedSpaceEntity space) {
        List<ConnectionDtos.SharedSpaceMemberResponse> members = sharedSpaceMemberRepository.findBySpaceIdOrderByJoinedAtAsc(space.getId()).stream()
                .map(member -> new ConnectionDtos.SharedSpaceMemberResponse(member.getUserEmail(), member.getRole())).toList();
        Map<String, ModulePermission> permissions = new LinkedHashMap<>();
        spacePermissionRepository.findBySpaceId(space.getId()).forEach(permission -> permissions.put(permission.getModuleKey(), permission.getPermission()));
        return new ConnectionDtos.SharedSpaceResponse(space.getId(), space.getName(), space.getDescription(), space.getOwnerEmail(), members, permissions);
    }

    private void updateRequestStatus(String actorEmail, Long requestId, ConnectionRequestStatus status, boolean incoming) {
        String actor = normalizeEmail(actorEmail);
        ConnectionRequestEntity request = requestRepository.findById(requestId).orElseThrow(() -> notFound("Solicitud no encontrada."));
        String expected = incoming ? request.getRecipientEmail() : request.getRequesterEmail();
        if (!actor.equals(normalizeEmail(expected)) || request.getStatus() != ConnectionRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes modificar esta solicitud.");
        }
        request.setStatus(status);
        request.setUpdatedAt(OffsetDateTime.now());
        requestRepository.save(request);
    }

    private ConnectionEntity getConnectionForActor(String actor, Long connectionId) {
        ConnectionEntity connection = connectionRepository.findById(connectionId).orElseThrow(() -> notFound("Conexión no encontrada."));
        if (!actor.equals(normalizeEmail(connection.getUserAEmail())) && !actor.equals(normalizeEmail(connection.getUserBEmail()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta conexión.");
        }
        return connection;
    }

    private ConnectionDtos.ConnectionResponse toConnectionResponse(ConnectionEntity connection, String actor) {
        String otherEmail = actor.equals(normalizeEmail(connection.getUserAEmail())) ? connection.getUserBEmail() : connection.getUserAEmail();
        UserEntity other = userRepository.findByEmail(otherEmail).orElse(null);
        Map<String, ModulePermission> byMe = new LinkedHashMap<>();
        Map<String, ModulePermission> toMe = new LinkedHashMap<>();
        permissionRepository.findByConnectionId(connection.getId()).forEach(permission -> {
            if (actor.equals(normalizeEmail(permission.getGrantedByEmail()))) byMe.put(permission.getModuleKey(), permission.getPermission());
            else toMe.put(permission.getModuleKey(), permission.getPermission());
        });
        return new ConnectionDtos.ConnectionResponse(connection.getId(), otherEmail,
                other == null ? "" : other.getName(), other == null ? "" : other.getLastname(),
                connection.getCreatedAt(), byMe, toMe);
    }

    private ConnectionDtos.ConnectionRequestResponse toRequestResponse(ConnectionRequestEntity request, String actor) {
        UserEntity requester = userRepository.findByEmail(request.getRequesterEmail()).orElse(null);
        UserEntity recipient = userRepository.findByEmail(request.getRecipientEmail()).orElse(null);
        return new ConnectionDtos.ConnectionRequestResponse(request.getId(), request.getRequesterEmail(),
                requester == null ? "" : requester.getName(), requester == null ? "" : requester.getLastname(),
                request.getRecipientEmail(), recipient == null ? "" : recipient.getName(), recipient == null ? "" : recipient.getLastname(),
                request.getStatus(), request.getCreatedAt(), normalizeEmail(request.getRecipientEmail()).equals(actor));
    }

    private ConnectionDtos.ConnectionRequestResponse toRequestResponse(ConnectionRequestEntity request, String actor, UserEntity recipient) {
        UserEntity requester = userRepository.findByEmail(request.getRequesterEmail()).orElse(null);
        return new ConnectionDtos.ConnectionRequestResponse(request.getId(), request.getRequesterEmail(),
                requester == null ? "" : requester.getName(), requester == null ? "" : requester.getLastname(),
                recipient.getEmail(), recipient.getName(), recipient.getLastname(), request.getStatus(), request.getCreatedAt(), false);
    }

    private Optional<ConnectionEntity> findConnection(String firstEmail, String secondEmail) {
        String[] pair = canonicalPair(firstEmail, secondEmail);
        return connectionRepository.findByUserAEmailAndUserBEmail(pair[0], pair[1]);
    }

    private boolean hasPendingRequestEitherWay(String firstEmail, String secondEmail) {
        return requestRepository.findFirstByRequesterEmailAndRecipientEmailAndStatusOrderByCreatedAtDesc(firstEmail, secondEmail, ConnectionRequestStatus.PENDING).isPresent()
                || requestRepository.findFirstByRequesterEmailAndRecipientEmailAndStatusOrderByCreatedAtDesc(secondEmail, firstEmail, ConnectionRequestStatus.PENDING).isPresent();
    }

    private boolean isBlockedEitherWay(String firstEmail, String secondEmail) {
        return blockedUserRepository.existsByBlockerEmailAndBlockedEmail(firstEmail, secondEmail)
                || blockedUserRepository.existsByBlockerEmailAndBlockedEmail(secondEmail, firstEmail);
    }

    private static String[] canonicalPair(String firstEmail, String secondEmail) {
        String first = normalizeEmail(firstEmail);
        String second = normalizeEmail(secondEmail);
        return first.compareTo(second) < 0 ? new String[]{first, second} : new String[]{second, first};
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeModule(String module) {
        String normalized = module == null ? "" : module.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_MODULES.contains(normalized)) throw badRequest("Módulo no válido.");
        return normalized;
    }

    private static boolean permissionAllows(ModulePermission actual, ModulePermission required) {
        return rank(actual) >= rank(required);
    }

    private static int rank(ModulePermission permission) {
        return switch (permission) {
            case NONE -> 0;
            case SUMMARY_READ -> 1;
            case DETAIL_READ -> 2;
            case EDIT -> 3;
            case ADMIN -> 4;
        };
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
