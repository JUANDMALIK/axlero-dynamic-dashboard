package com.axlero.dashboard.service;

import com.axlero.dashboard.config.CacheConfig;
import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.Dataset;
import com.axlero.dashboard.repository.DashboardDatasetRepository;
import com.axlero.dashboard.repository.DatasetRepository;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DatasetServiceCacheTest {

    private AnnotationConfigApplicationContext context;
    private DatasetRepository datasetRepository;
    private DashboardService dashboardService;
    private DatasetService datasetService;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        datasetRepository = mock(DatasetRepository.class);
        dashboardService = mock(DashboardService.class);
        DashboardDatasetRepository dashboardDatasetRepository =
                mock(DashboardDatasetRepository.class);
        WidgetConfigRepository widgetConfigRepository =
                mock(WidgetConfigRepository.class);

        context = new AnnotationConfigApplicationContext();

        context.registerBean(
                "datasetRepository",
                DatasetRepository.class,
                () -> datasetRepository
        );

        context.registerBean(
                "dashboardService",
                DashboardService.class,
                () -> dashboardService
        );

        context.registerBean(
                "dashboardDatasetRepository",
                DashboardDatasetRepository.class,
                () -> dashboardDatasetRepository
        );

        context.registerBean(
                "widgetConfigRepository",
                WidgetConfigRepository.class,
                () -> widgetConfigRepository
        );

        context.registerBean(
                "objectMapper",
                ObjectMapper.class,
                () -> new ObjectMapper()
        );

        context.register(TestCachingConfig.class);
        context.refresh();

        datasetService = context.getBean(DatasetService.class);
        cacheManager = context.getBean(CacheManager.class);

        Dashboard dashboard = mock(Dashboard.class);
        when(dashboard.getId()).thenReturn(1L);

        Dataset dataset = mock(Dataset.class);
        when(dataset.getId()).thenReturn(1L);
        when(dataset.getFilename()).thenReturn("sample.csv");
        when(dataset.getCsvContent()).thenReturn(
                "product,sales\nLaptop,100\nMobile,200"
        );
        when(dataset.getRowCount()).thenReturn(2);
        when(dataset.getUploadedAt()).thenReturn(null);

        when(dashboardService.getOrCreateDashboard("junaid"))
                .thenReturn(dashboard);

        when(datasetRepository.findFirstByDashboardIdOrderByUploadedAtDesc(1L))
                .thenReturn(Optional.of(dataset));
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void analyticsShouldUseCacheOnSecondCall() {
        datasetService.analytics("junaid");
        datasetService.analytics("junaid");

        verify(datasetRepository, times(1))
                .findFirstByDashboardIdOrderByUploadedAtDesc(1L);
    }

    @Test
    void uploadingDatasetShouldEvictAnalyticsCache() {
        datasetService.analytics("junaid");

        Cache cache = cacheManager.getCache("datasetAnalytics");
        assertNotNull(cache);
        assertNotNull(cache.get("junaid"));

        Dataset savedDataset = mock(Dataset.class);

        when(savedDataset.getId()).thenReturn(2L);
        when(savedDataset.getFilename()).thenReturn("new.csv");
        when(savedDataset.getRowCount()).thenReturn(1);
        when(savedDataset.getUploadedAt()).thenReturn(null);

        when(datasetRepository.save(any(Dataset.class)))
                .thenReturn(savedDataset);

        datasetService.upload(
                "junaid",
                "new.csv",
                "product,sales\nTablet,300".getBytes(StandardCharsets.UTF_8)
        );

        assertNull(cache.get("junaid"));
    }

    @Configuration
    @EnableCaching
    @Import(CacheConfig.class)
    static class TestCachingConfig {

        @Bean
        DatasetService datasetService(
                DatasetRepository repository,
                DashboardService dashboardService,
                ObjectMapper mapper,
                DashboardDatasetRepository dashboardDatasetRepository,
                WidgetConfigRepository widgetConfigRepository) {

            return new DatasetService(
                    dashboardService,
                    dashboardDatasetRepository,
                    widgetConfigRepository,
                    mapper,
                    repository
            );
        }
    }
}
