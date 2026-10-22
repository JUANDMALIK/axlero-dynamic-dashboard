package com.axlero.dashboard.controller;

import com.axlero.dashboard.dto.WorkspaceCreateRequest;
import com.axlero.dashboard.dto.WorkspaceResponse;
import com.axlero.dashboard.service.WorkspaceService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse createWorkspace(
            @RequestBody WorkspaceCreateRequest request,
            Authentication authentication) {
        return workspaceService.createWorkspace(
                authentication.getName(), request);
    }

    @GetMapping
    public List<WorkspaceResponse> getMyWorkspaces(
            Authentication authentication) {
        return workspaceService.getMyWorkspaces(
                authentication.getName());
    }
}