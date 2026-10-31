package com.axlero.dashboard.service;

import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.entity.WidgetConfig;
import com.axlero.dashboard.repository.DashboardRepository;
import com.axlero.dashboard.repository.UserRepository;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class DashboardExportService {

    private static final List<String> HEADERS = List.of(
            "Dashboard Name", "Widget Title", "Chart Type",
            "Metric Type", "Group By Field", "Value Field", "Position"
    );

    private final DashboardRepository dashboardRepository;
    private final WidgetConfigRepository widgetConfigRepository;
    private final UserRepository userRepository;

    public DashboardExportService(
            DashboardRepository dashboardRepository,
            WidgetConfigRepository widgetConfigRepository,
            UserRepository userRepository) {
        this.dashboardRepository = dashboardRepository;
        this.widgetConfigRepository = widgetConfigRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(String username) {
        Dashboard dashboard = getUserDashboard(username);
        List<WidgetConfig> widgets =
                widgetConfigRepository.findByDashboardIdOrderByPositionAsc(dashboard.getId());

        StringBuilder csv = new StringBuilder();
        appendCsvRow(csv, HEADERS);

        if (widgets.isEmpty()) {
            appendCsvRow(csv, List.of(dashboard.getName(), "", "", "", "", "", ""));
        } else {
            for (WidgetConfig widget : widgets) {
                appendCsvRow(csv, getWidgetValues(dashboard, widget));
            }
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(String username) {
        Dashboard dashboard = getUserDashboard(username);
        List<WidgetConfig> widgets =
                widgetConfigRepository.findByDashboardIdOrderByPositionAsc(dashboard.getId());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Dashboard");
            writeRow(sheet.createRow(0), HEADERS);

            if (widgets.isEmpty()) {
                writeRow(sheet.createRow(1),
                        List.of(dashboard.getName(), "", "", "", "", "", ""));
            } else {
                for (int i = 0; i < widgets.size(); i++) {
                    writeRow(sheet.createRow(i + 1),
                            getWidgetValues(dashboard, widgets.get(i)));
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

    private Dashboard getUserDashboard(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Authenticated user was not found"));

        return dashboardRepository.findFirstByCreatedByOrderByIdAsc(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Dashboard was not found"));
    }

    private List<String> getWidgetValues(Dashboard dashboard, WidgetConfig widget) {
        return List.of(
                safe(dashboard.getName()),
                safe(widget.getTitle()),
                safe(widget.getChartType()),
                safe(widget.getMetricType()),
                safe(widget.getGroupByField()),
                safe(widget.getValueField()),
                widget.getPosition() == null ? "" : widget.getPosition().toString()
        );
    }

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) csv.append(',');

            String value = values.get(i);
            String trimmed = value.stripLeading();

            if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
                value = "'" + value;
            }

            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private void writeRow(Row row, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            row.createCell(i).setCellValue(values.get(i));
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}