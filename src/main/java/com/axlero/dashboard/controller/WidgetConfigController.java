package com.axlero.dashboard.controller;

import com.axlero.dashboard.dto.WidgetConfigRequest;
import com.axlero.dashboard.dto.WidgetConfigResponse;
import com.axlero.dashboard.service.WidgetConfigService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/widgets")
public class WidgetConfigController {
    private final WidgetConfigService widgetConfigService;

    public WidgetConfigController(WidgetConfigService widgetConfigService) {
        this.widgetConfigService = widgetConfigService;
    }

    @GetMapping
    public List<WidgetConfigResponse> getWidgets(Authentication authentication) {
        return widgetConfigService.list(authentication.getName());
    }

    @PutMapping
    public List<WidgetConfigResponse> replaceWidgets(
            Authentication authentication,
            @Valid @RequestBody List<@Valid WidgetConfigRequest> widgets) {
        return widgetConfigService.replace(authentication.getName(), widgets);
    }
}
