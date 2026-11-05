package com.finanzapp.tags.service.impl;

import com.finanzapp.tags.repository.TagDeletionOperationRepository;
import com.finanzapp.tags.repository.TagRepository;
import com.finanzapp.tags.repository.entities.TagDeletionOperationEntity;
import com.finanzapp.tags.repository.entities.TagDeletionState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagDeletionAttemptService {

    private final TagDeletionOperationRepository operationRepository;
    private final TagRepository tagRepository;
    private final MovementsClientImpl movementsClient;

    @Transactional(transactionManager = "tagsTransactionManager")
    public void process(Long operationId) {
        TagDeletionOperationEntity operation = operationRepository.findById(operationId)
                .orElseThrow(() -> new IllegalArgumentException("Operación de borrado no encontrada"));

        if (operation.getState() == TagDeletionState.COMPLETED) return;

        // El endpoint remoto es idempotente: repetirlo solo deja a null las
        // referencias que todavía apunten a esta etiqueta.
        movementsClient.detachMovements(operation.getUserEmail(), operation.getTagId());

        tagRepository.findByIdAndUserEmail(operation.getTagId(), operation.getUserEmail())
                .ifPresent(tagRepository::delete);

        operation.setState(com.finanzapp.tags.repository.entities.TagDeletionState.COMPLETED);
        operation.setLastError(null);
        operationRepository.save(operation);
    }
}
