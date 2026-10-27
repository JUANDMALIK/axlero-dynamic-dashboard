package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.ChartDataPoint;
import com.axlero.dashboard.dto.WidgetResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class WidgetExportService {

    private static final List<String> HEADERS = List.of(
            "Widget", "Metric", "Chart type", "Group", "Value");

    private final DatasetService datasetService;

    public WidgetExportService(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    public byte[] exportCsv() {
        StringBuilder csv = new StringBuilder("\uFEFF");
        appendCsvRow(csv, HEADERS);
        datasetService.getMyWidgetResults().forEach(result ->
                result.data().forEach(point -> appendCsvRow(csv, values(result.widget(), point))));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportExcel() {
        var results = datasetService.getMyWidgetResults();
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Dashboard results");
            writeRow(sheet.createRow(0), HEADERS);

            int rowIndex = 1;
            for (var result : results) {
                for (ChartDataPoint point : result.data()) {
                    Row row = sheet.createRow(rowIndex++);
                    writeRow(row, values(result.widget(), point));
                }
            }

            for (int column = 0; column < HEADERS.size(); column++) {
                sheet.autoSizeColumn(column);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the Excel export", exception);
        }
    }

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append('"')
                    .append(csvSafe(values.get(index)).replace("\"", "\"\""))
                    .append('"');
        }
        csv.append("\r\n");
    }

    private String csvSafe(String value) {
        int firstContent = 0;
        while (firstContent < value.length()
                && Character.isWhitespace(value.charAt(firstContent))) {
            firstContent++;
        }
        if (firstContent < value.length()
                && "=+-@".indexOf(value.charAt(firstContent)) >= 0) {
            return "'" + value;
        }
        return value;
    }

    private List<String> values(WidgetResponse widget, ChartDataPoint point) {
        return List.of(
                widget.title(),
                widget.metricType(),
                widget.chartType(),
                point.name(),
                Double.toString(point.value()));
    }

    private void writeRow(Row row, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            row.createCell(index).setCellValue(values.get(index));
        }
    }
}
