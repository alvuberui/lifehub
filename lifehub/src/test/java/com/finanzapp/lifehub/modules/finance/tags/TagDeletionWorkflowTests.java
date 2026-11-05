package com.finanzapp.lifehub.modules.finance.tags;

import com.finanzapp.tags.repository.TagDeletionOperationRepository;
import com.finanzapp.tags.repository.TagRepository;
import com.finanzapp.tags.repository.entities.TagDeletionOperationEntity;
import com.finanzapp.tags.repository.entities.TagDeletionState;
import com.finanzapp.tags.repository.entities.TagEntity;
import com.finanzapp.tags.repository.mapper.TagEntityMapper;
import com.finanzapp.tags.service.impl.MovementsClientImpl;
import com.finanzapp.tags.service.impl.TagDeletionAttemptService;
import com.finanzapp.tags.service.impl.TagServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagDeletionWorkflowTests {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TagDeletionOperationRepository operationRepository;

    @Mock
    private TagEntityMapper tagEntityMapper;

    @Mock
    private MovementsClientImpl movementsClient;

    @Test
    void deleteMarksTagAndOperationInTheSameLocalWorkflow() {
        TagEntity tag = TagEntity.builder()
                .id(10L)
                .name("Casa")
                .color("#FFFFFF")
                .userEmail("user@example.com")
                .build();
        when(tagRepository.findByIdAndUserEmail(10L, "user@example.com")).thenReturn(Optional.of(tag));

        new TagServiceImpl(tagRepository, tagEntityMapper, operationRepository)
                .deleteTag(10L, "user@example.com");

        assertThat(tag.isDeleted()).isTrue();
        verify(tagRepository).save(tag);
        verify(operationRepository).save(any(TagDeletionOperationEntity.class));
        verifyNoInteractions(movementsClient);
    }

    @Test
    void processingDetachesMovementsBeforePhysicallyDeletingTag() {
        TagDeletionOperationEntity operation = TagDeletionOperationEntity.builder()
                .id(1L)
                .tagId(10L)
                .userEmail("user@example.com")
                .state(TagDeletionState.PENDING)
                .attempts(0)
                .nextAttemptAt(Instant.now())
                .build();
        TagEntity tag = TagEntity.builder()
                .id(10L)
                .userEmail("user@example.com")
                .deleted(true)
                .build();
        when(operationRepository.findById(1L)).thenReturn(Optional.of(operation));
        when(tagRepository.findByIdAndUserEmail(10L, "user@example.com")).thenReturn(Optional.of(tag));

        new TagDeletionAttemptService(operationRepository, tagRepository, movementsClient).process(1L);

        var inOrder = inOrder(movementsClient, tagRepository, operationRepository);
        inOrder.verify(movementsClient).detachMovements("user@example.com", 10L);
        inOrder.verify(tagRepository).delete(tag);
        inOrder.verify(operationRepository).save(operation);
        assertThat(operation.getState()).isEqualTo(TagDeletionState.COMPLETED);
    }
}
