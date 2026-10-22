package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.WorkspaceCreateRequest;
import com.axlero.dashboard.dto.WorkspaceResponse;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.entity.Workspace;
import com.axlero.dashboard.repository.UserRepository;
import com.axlero.dashboard.repository.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WorkspaceResponse createWorkspace(
            String username,
            WorkspaceCreateRequest request) {

        if (request == null || request.name() == null
                || request.name().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Workspace name is required");
        }

        if (request.name().trim().length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Workspace name must not exceed 100 characters");
        }

        if (request.description() != null
                && request.description().length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Description must not exceed 500 characters");
        }

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        Workspace workspace = new Workspace();
        workspace.setName(request.name().trim());
        workspace.setDescription(request.description());
        workspace.setOwner(owner);

        return toResponse(workspaceRepository.save(workspace));
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getMyWorkspaces(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        return workspaceRepository.findByOwnerId(owner.getId())
                .stream()
                .map(WorkspaceService::toResponse)
                .toList();
    }

    private static WorkspaceResponse toResponse(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getCreatedAt());
    }
}