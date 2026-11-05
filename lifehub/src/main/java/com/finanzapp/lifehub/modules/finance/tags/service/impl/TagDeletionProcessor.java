package com.finanzapp.tags.service.impl;

import com.finanzapp.tags.repository.TagDeletionOperationRepository;
import com.finanzapp.tags.repository.entities.TagDeletionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagDeletionProcessor {

    private final TagDeletionOperationRepository operationRepository;
    private final TagDeletionAttemptService attemptService;
    private final TagDeletionFailureService failureService;

    @Scheduled(
            fixedDelayString = "${services.tag-deletion-processor.fixed-delay-ms:5000}",
            initialDelayString = "${services.tag-deletion-processor.initial-delay-ms:5000}"
    )
    public void processPendingOperations() {
        operationRepository
                .findTop20ByStateAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(TagDeletionState.PENDING, Instant.now())
                .forEach(operation -> {
                    try {
                        attemptService.process(operation.getId());
                    } catch (Exception exception) {
                        log.warn("No se pudo completar el borrado de la etiqueta {}", operation.getTagId(), exception);
                        failureService.registerFailure(operation.getId(), exception);
                    }
                });
    }
}
