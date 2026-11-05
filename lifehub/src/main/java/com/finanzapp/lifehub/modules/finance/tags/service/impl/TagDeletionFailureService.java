package com.finanzapp.tags.service.impl;

import com.finanzapp.tags.repository.TagDeletionOperationRepository;
import com.finanzapp.tags.repository.entities.TagDeletionOperationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TagDeletionFailureService {

    private final TagDeletionOperationRepository operationRepository;

    @Transactional(transactionManager = "tagsTransactionManager")
    public void registerFailure(Long operationId, Exception exception) {
        operationRepository.findById(operationId).ifPresent(operation -> {
            operation.setAttempts(operation.getAttempts() + 1);
            long delaySeconds = Math.min(300, 5L * (1L << Math.min(operation.getAttempts() - 1, 6)));
            operation.setNextAttemptAt(Instant.now().plus(Duration.ofSeconds(delaySeconds)));
            operation.setLastError(safeMessage(exception));
            operationRepository.save(operation);
        });
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return exception.getClass().getSimpleName();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
