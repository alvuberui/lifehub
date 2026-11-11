package com.finanzapp.lifehub.modules.connections;

import com.finanzapp.lifehub.modules.connections.api.ConnectionDtos;
import com.finanzapp.lifehub.modules.connections.domain.ModulePermission;
import com.finanzapp.lifehub.modules.connections.repository.BlockedUserRepository;
import com.finanzapp.lifehub.modules.connections.repository.ConnectionRepository;
import com.finanzapp.lifehub.modules.connections.repository.ConnectionRequestRepository;
import com.finanzapp.lifehub.modules.connections.repository.ModulePermissionRepository;
import com.finanzapp.lifehub.modules.connections.repository.SharedSpaceMemberRepository;
import com.finanzapp.lifehub.modules.connections.repository.SharedSpaceRepository;
import com.finanzapp.lifehub.modules.connections.repository.SpaceModulePermissionRepository;
import com.finanzapp.lifehub.modules.connections.repository.entities.ConnectionEntity;
import com.finanzapp.lifehub.modules.connections.repository.entities.ModulePermissionEntity;
import com.finanzapp.lifehub.modules.connections.service.impl.ConnectionsServiceImpl;
import com.finanzapp.lifehub.modules.identity.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionsServiceDirectionalPermissionTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ConnectionRequestRepository requestRepository;
    @Mock
    private ConnectionRepository connectionRepository;
    @Mock
    private ModulePermissionRepository permissionRepository;
    @Mock
    private BlockedUserRepository blockedUserRepository;
    @Mock
    private SharedSpaceRepository sharedSpaceRepository;
    @Mock
    private SharedSpaceMemberRepository sharedSpaceMemberRepository;
    @Mock
    private SpaceModulePermissionRepository spacePermissionRepository;

    @InjectMocks
    private ConnectionsServiceImpl service;

    @Test
    void keepsOnePermissionPerDirectionWhenBothUsersShareTheSameModule() {
        ConnectionEntity connection = ConnectionEntity.builder()
                .id(7L)
                .userAEmail("alvaro@example.com")
                .userBEmail("cristina@example.com")
                .build();
        when(connectionRepository.findById(7L)).thenReturn(Optional.of(connection));
        when(permissionRepository.findByConnectionIdAndModuleKeyAndGrantedByEmail(7L, "finance", "alvaro@example.com"))
                .thenReturn(Optional.empty());
        when(permissionRepository.findByConnectionIdAndModuleKeyAndGrantedByEmail(7L, "finance", "cristina@example.com"))
                .thenReturn(Optional.empty());
        when(permissionRepository.save(any(ModulePermissionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConnectionDtos.PermissionResponse alvaroPermission = service.updatePermission(
                "alvaro@example.com", 7L, "finance", ModulePermission.DETAIL_READ);
        ConnectionDtos.PermissionResponse cristinaPermission = service.updatePermission(
                "cristina@example.com", 7L, "finance", ModulePermission.SUMMARY_READ);

        ArgumentCaptor<ModulePermissionEntity> captor = ArgumentCaptor.forClass(ModulePermissionEntity.class);
        verify(permissionRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        assertThat(alvaroPermission.permission()).isEqualTo(ModulePermission.DETAIL_READ);
        assertThat(cristinaPermission.permission()).isEqualTo(ModulePermission.SUMMARY_READ);
        assertThat(captor.getAllValues()).extracting(ModulePermissionEntity::getGrantedByEmail)
                .containsExactly("alvaro@example.com", "cristina@example.com");
    }
}
