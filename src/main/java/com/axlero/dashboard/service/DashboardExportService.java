package com.axlero.dashboard.service;

import com.axlero.dashboard.api.DashboardResponse;
import com.axlero.dashboard.api.WidgetConfigResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class DashboardExportService {

    private static final List<String> HEADERS = List.of(
            "Dashboard name",
            "Widget title",
            "Chart type",
            "Metric type",
            "Group by field",
            "Value field",
            "Position");

    public byte[] toCsv(DashboardResponse dashboard) {
        StringBuilder csv = new StringBuilder();
        appendCsvRow(csv, HEADERS);
        if (dashboard.widgets().isEmpty()) {
            appendCsvRow(csv, List.of(dashboard.name(), "", "", "", "", "", ""));
        } else {
            dashboard.widgets().forEach(widget -> appendCsvRow(csv, values(dashboard, widget)));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toExcel(DashboardResponse dashboard) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Dashboard");
            writeRow(sheet.createRow(0), HEADERS);
            if (dashboard.widgets().isEmpty()) {
                writeRow(sheet.createRow(1), List.of(dashboard.name(), "", "", "", "", "", ""));
            } else {
                for (int i = 0; i < dashboard.widgets().size(); i++) {
                    writeRow(sheet.createRow(i + 1), values(dashboard, dashboard.widgets().get(i)));
                }
            }
            for (int i = 0; i < HEADERS.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not generate Excel export", exception);
        }
    }

    private List<String> values(DashboardResponse dashboard, WidgetConfigResponse widget) {
        return List.of(
                dashboard.name(),
                widget.title(),
                widget.chartType(),
                widget.metricType(),
                valueOrEmpty(widget.groupByField()),
                widget.valueField(),
                widget.position().toString());
    }

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            String value = protectCsvFormula(values.get(i));
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private String protectCsvFormula(String value) {
        String trimmed = value.stripLeading();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
            return "'" + value;
        }
        return value;
    }

    private void writeRow(Row row, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            row.createCell(i).setCellValue(values.get(i));
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
