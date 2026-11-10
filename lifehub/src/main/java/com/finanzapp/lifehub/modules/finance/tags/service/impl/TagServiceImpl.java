package com.finanzapp.tags.service.impl;

import com.finanzapp.tags.repository.TagRepository;
import com.finanzapp.tags.repository.entities.TagEntity;
import com.finanzapp.tags.repository.entities.TagDeletionOperationEntity;
import com.finanzapp.tags.repository.TagDeletionOperationRepository;
import com.finanzapp.tags.repository.mapper.TagEntityMapper;
import com.finanzapp.tags.service.TagService;
import com.finanzapp.tags.service.domain.CreateTagRequest;
import com.finanzapp.tags.service.domain.Tag;
import com.finanzapp.lifehub.modules.connections.domain.ModulePermission;
import com.finanzapp.lifehub.modules.connections.service.ConnectionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class TagServiceImpl implements TagService {

    private final TagRepository movementRepository;

    private final TagEntityMapper tagEntityMapper;

    private final TagDeletionOperationRepository deletionOperationRepository;
    private final ConnectionsService connectionsService;

    @Autowired
    public TagServiceImpl(
            TagRepository movementRepository,
            TagEntityMapper tagEntityMapper,
            TagDeletionOperationRepository deletionOperationRepository,
            ConnectionsService connectionsService) {
        this.movementRepository = movementRepository;
        this.tagEntityMapper = tagEntityMapper;
        this.deletionOperationRepository = deletionOperationRepository;
        this.connectionsService = connectionsService;
    }

    public TagServiceImpl(
            TagRepository movementRepository,
            TagEntityMapper tagEntityMapper,
            TagDeletionOperationRepository deletionOperationRepository) {
        this(movementRepository, tagEntityMapper, deletionOperationRepository, null);
    }



    @Override
    public Tag createTag(CreateTagRequest createTagRequest, String userEmail) {
        String name = createTagRequest.getName().trim();
        String color = createTagRequest.getColor().trim().toUpperCase(Locale.ROOT);
        CreateTagRequest normalized = CreateTagRequest.builder().name(name).color(color).build();

        if (!movementRepository.existsByNameAndUserEmailAndDeletedFalse(name, userEmail)) {

            TagEntity savedEntity = movementRepository.save(tagEntityMapper.toEntity(normalized, userEmail));

            return tagEntityMapper.toDomain(savedEntity);
        }
        throw new IllegalArgumentException("La etiqueta con nombre " + createTagRequest.getName() + " ya existe.");
    }

    @Override
    public List<Tag> getTagsByUserEmail(String userEmail) {
        List<TagEntity> tagEntities = movementRepository.findByUserEmailAndDeletedFalse(userEmail);
        return tagEntities.stream()
                .map(tagEntityMapper::toDomain)
                .toList();
    }

    @Override
    public Tag getTagByIdAndUserEmail(Long tagId, String userEmail) {
        TagEntity tagEntity = movementRepository.findByIdAndUserEmailAndDeletedFalse(tagId, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("La etiqueta con id " + tagId + " no existe para el usuario " + userEmail + "."));
        return tagEntityMapper.toDomain(tagEntity);
    }

    @Override
    @Transactional(transactionManager = "tagsTransactionManager")
    public void deleteTag(Long tagId, String userEmail) {
        TagEntity tagEntity = movementRepository.findByIdAndUserEmail(tagId, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("La etiqueta con id " + tagId + " no existe para el usuario " + userEmail + "."));

        // La transición visible es local y atómica. La limpieza de movimientos se
        // procesa después mediante una operación idempotente reintentable.
        if (tagEntity.isDeleted()) {
            deletionOperationRepository.findByTagIdAndUserEmail(tagId, userEmail)
                    .orElseGet(() -> deletionOperationRepository.save(
                            TagDeletionOperationEntity.pending(tagId, userEmail)
                    ));
            return;
        }

        tagEntity.setDeleted(true);
        movementRepository.save(tagEntity);
        deletionOperationRepository.save(TagDeletionOperationEntity.pending(tagId, userEmail));
    }

    @Override
    public Boolean existsByIdAndUserEmail(Long tagId, String userEmail) {
        return movementRepository.existsByIdAndUserEmailAndDeletedFalse(tagId, userEmail);
    }

    @Override
    public List<Tag> getTagsByIdsAndUserEmail(List<Long> ids, String email) {
        if (ids == null || ids.isEmpty()) return List.of();

        return movementRepository.findByUserEmailAndIdInAndDeletedFalse(email, ids).stream()
                .map(tagEntityMapper::toDomain)
                .toList();
    }

    @Override
    public Tag createTagForOwner(CreateTagRequest createTagRequest, String ownerEmail, String actorEmail) {
        requireAdmin(ownerEmail, actorEmail);
        return createTag(createTagRequest, ownerEmail);
    }

    @Override
    public List<Tag> getTagsForOwner(String ownerEmail, String actorEmail) {
        if (!ownerEmail.equalsIgnoreCase(actorEmail)
                && !connectionsService.hasPermission(ownerEmail, actorEmail, "finance", ModulePermission.DETAIL_READ)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "No tienes permiso para ver estas etiquetas.");
        }
        return getTagsByUserEmail(ownerEmail);
    }

    @Override
    public void deleteTagForOwner(Long tagId, String ownerEmail, String actorEmail) {
        requireAdmin(ownerEmail, actorEmail);
        deleteTag(tagId, ownerEmail);
    }

    private void requireAdmin(String ownerEmail, String actorEmail) {
        if (!ownerEmail.equalsIgnoreCase(actorEmail)
                && !connectionsService.hasPermission(ownerEmail, actorEmail, "finance", ModulePermission.ADMIN)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Necesitas permiso de administración para gestionar estas etiquetas.");
        }
    }

}
