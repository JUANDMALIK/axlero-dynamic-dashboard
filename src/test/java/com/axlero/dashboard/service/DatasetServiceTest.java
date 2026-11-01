package com.axlero.dashboard.service;

import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.DashboardDataset;
import com.axlero.dashboard.entity.WidgetConfig;
import com.axlero.dashboard.repository.DashboardDatasetRepository;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetServiceTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private DashboardDatasetRepository datasetRepository;

    @Mock
    private WidgetConfigRepository widgetConfigRepository;

    private DatasetService datasetService;
    private Dashboard dashboard;
    private DashboardDataset storedDataset;

    private void stubDashboardAndDataset() {
        datasetService = new DatasetService(
                dashboardService,
                datasetRepository,
                widgetConfigRepository,
                new ObjectMapper());
        dashboard = new Dashboard();
        ReflectionTestUtils.setField(dashboard, "id", 8L);
        when(dashboardService.getMyDashboard()).thenReturn(dashboard);
        when(datasetRepository.findByDashboardId(8L))
                .thenAnswer(invocation -> Optional.ofNullable(storedDataset));
    }

    private void stubDatasetSave() {
        when(datasetRepository.save(any(DashboardDataset.class)))
                .thenAnswer(invocation -> {
                    storedDataset = invocation.getArgument(0);
                    return storedDataset;
                });
    }

    @Test
    void uploadsCsvAndAggregatesNumericMetricsFromSelectedColumns() {
        stubDashboardAndDataset();
        stubDatasetSave();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sales.csv",
                "text/csv",
                "\"Month name\",\"Revenue, USD\"\nJune,10\nJune,20\nJuly,5\n"
                        .getBytes(StandardCharsets.UTF_8));

        var response = datasetService.uploadDataset(file);

        assertEquals("sales.csv", response.name());
        assertEquals(3, response.rowCount());
        assertEquals("Revenue, USD", response.columns().get(1));

        WidgetConfig widget = new WidgetConfig();
        ReflectionTestUtils.setField(widget, "id", 12L);
        widget.setMetricType("SUM");
        widget.setValueField("Revenue, USD");
        widget.setGroupByField("Month name");
        widget.setDashboard(dashboard);
        when(widgetConfigRepository.findById(12L)).thenReturn(Optional.of(widget));

        var data = datasetService.getWidgetData(12L);

        assertEquals(2, data.size());
        assertEquals("June", data.get(0).name());
        assertEquals(30.0, data.get(0).value());
        assertEquals("July", data.get(1).name());
        assertEquals(5.0, data.get(1).value());
    }

    @Test
    void rejectsDuplicateColumnNamesIgnoringCase() {
        datasetService = new DatasetService(
                dashboardService,
                datasetRepository,
                widgetConfigRepository,
                new ObjectMapper());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "duplicate.csv",
                "text/csv",
                "Amount,amount\n1,2\n".getBytes(StandardCharsets.UTF_8));

        assertThrows(ResponseStatusException.class, () -> datasetService.uploadDataset(file));
    }

    @Test
    void uploadsFirstWorksheetFromXlsx() throws Exception {
        stubDashboardAndDataset();
        stubDatasetSave();
        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Data");
            var headers = sheet.createRow(0);
            headers.createCell(0).setCellValue("Region");
            headers.createCell(1).setCellValue("Sales");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("West");
            row.createCell(1).setCellValue(17.5);
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sales.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookBytes);

        var response = datasetService.uploadDataset(file);

        assertEquals("sales.xlsx", response.name());
        assertEquals(1, response.rowCount());
        assertEquals("Sales", response.columns().get(1));
    }

    @Test
    void rejectsNonNumericValuesForSumMetrics() {
        stubDashboardAndDataset();
        storedDataset = new DashboardDataset();
        storedDataset.setDatasetName("sales.csv");
        storedDataset.setColumnsJson("[\"amount\"]");
        storedDataset.setRowsJson("[[\"not-a-number\"]]");
        storedDataset.setDashboard(dashboard);

        WidgetConfig widget = new WidgetConfig();
        widget.setMetricType("SUM");
        widget.setValueField("amount");
        widget.setDashboard(dashboard);
        when(widgetConfigRepository.findById(12L)).thenReturn(Optional.of(widget));

        assertThrows(ResponseStatusException.class, () -> datasetService.getWidgetData(12L));
    }

    @Test
    void calculatesAverageAndCountOverNonEmptyValues() {
        stubDashboardAndDataset();
        storedDataset = new DashboardDataset();
        storedDataset.setDatasetName("values.csv");
        storedDataset.setColumnsJson("[\"amount\"]");
        storedDataset.setRowsJson("[[\"2\"],[\"4\"],[\"\"]]");
        storedDataset.setDashboard(dashboard);

        WidgetConfig widget = new WidgetConfig();
        widget.setMetricType("AVERAGE");
        widget.setValueField("amount");
        widget.setDashboard(dashboard);
        when(widgetConfigRepository.findById(12L)).thenReturn(Optional.of(widget));

        assertEquals(3.0, datasetService.getWidgetData(12L).get(0).value());

        widget.setMetricType("COUNT");
        assertEquals(2.0, datasetService.getWidgetData(12L).get(0).value());
    }
}
