package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.WorkspaceCreateRequest;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.entity.Workspace;
import com.axlero.dashboard.repository.UserRepository;
import com.axlero.dashboard.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void newWorkspaceUsesTheCanonicalUserEntityAsOwner() {
        User owner = new User();
        ReflectionTestUtils.setField(owner, "id", 18L);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(any(Workspace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        workspaceService.createWorkspace(
                "alice", new WorkspaceCreateRequest("Team", "Private workspace"));

        ArgumentCaptor<Workspace> captor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(captor.capture());
        assertEquals(owner, captor.getValue().getOwner());
        assertEquals(18L, captor.getValue().getOwner().getId());
    }
}
