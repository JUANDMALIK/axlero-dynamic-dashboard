package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.ChartDataPoint;
import com.axlero.dashboard.dto.WidgetResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WidgetExportServiceTest {

    @Mock
    private DatasetService datasetService;

    @InjectMocks
    private WidgetExportService widgetExportService;

    @Test
    void csvExportContainsCalculatedResultsAndEscapesFormulasAndQuotes() {
        when(datasetService.getMyWidgetResults()).thenReturn(List.of(
                new DatasetService.WidgetResult(
                        new WidgetResponse(1L, "Monthly \"sales\"", "BAR", "SUM", "month", "amount", 0),
                        List.of(new ChartDataPoint("June, 2026", 12.5))),
                new DatasetService.WidgetResult(
                        new WidgetResponse(2L, " =1+1", "LINE", "COUNT", null, "id", 1),
                        List.of(new ChartDataPoint("All rows", 3)))));

        String csv = new String(widgetExportService.exportCsv(), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Monthly \"\"sales\"\"\""));
        assertTrue(csv.contains("\"' =1+1\""));
        assertTrue(csv.contains("\"June, 2026\",\"12.5\""));
        assertTrue(csv.contains("\"SUM\",\"BAR\",\"June, 2026\",\"12.5\""));
    }

    @Test
    void excelExportIsAReadableWorkbookWithCalculatedResults() throws Exception {
        when(datasetService.getMyWidgetResults()).thenReturn(List.of(
                new DatasetService.WidgetResult(
                        new WidgetResponse(1L, "Monthly sales", "BAR", "SUM", "month", "amount", 0),
                        List.of(new ChartDataPoint("June", 42.0)))));

        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(widgetExportService.exportExcel()))) {
            assertEquals(
                    "Monthly sales",
                    workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals(
                    "June",
                    workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue());
            assertEquals(
                    "42.0",
                    workbook.getSheetAt(0).getRow(1).getCell(4).getStringCellValue());
        }
    }
}
