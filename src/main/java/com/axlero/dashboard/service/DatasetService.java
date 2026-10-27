package com.axlero.dashboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.axlero.dashboard.dto.ChartDataPoint;
import com.axlero.dashboard.dto.DatasetResponse;
import com.axlero.dashboard.dto.WidgetResponse;
import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.DashboardDataset;
import com.axlero.dashboard.entity.WidgetConfig;
import com.axlero.dashboard.repository.DashboardDatasetRepository;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DatasetService {

    private static final int MAX_COLUMNS = 100;
    private static final int MAX_ROWS = 50_000;
    private static final TypeReference<List<List<String>>> ROWS_TYPE =
            new TypeReference<>() { };
    private static final TypeReference<List<String>> COLUMNS_TYPE =
            new TypeReference<>() { };

    private final DashboardService dashboardService;
    private final DashboardDatasetRepository datasetRepository;
    private final WidgetConfigRepository widgetConfigRepository;
    private final ObjectMapper objectMapper;

    public DatasetService(
            DashboardService dashboardService,
            DashboardDatasetRepository datasetRepository,
            WidgetConfigRepository widgetConfigRepository,
            ObjectMapper objectMapper) {
        this.dashboardService = dashboardService;
        this.datasetRepository = datasetRepository;
        this.widgetConfigRepository = widgetConfigRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DatasetResponse getDataset() {
        Dashboard dashboard = dashboardService.getMyDashboard();
        return datasetRepository.findByDashboardId(dashboard.getId())
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No dataset has been uploaded"));
    }

    @Transactional
    public DatasetResponse uploadDataset(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a non-empty CSV or XLSX file");
        }

        String filename = safeFilename(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(filename);
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only CSV and XLSX files are supported");
        }

        ParsedDataset parsed;
        try {
            parsed = switch (extension.toLowerCase(Locale.ROOT)) {
                case "csv" -> parseCsv(file);
                case "xlsx" -> parseExcel(file);
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Only CSV and XLSX files are supported");
            };
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The uploaded file could not be read");
        }

        Dashboard dashboard = dashboardService.getMyDashboard();
        DashboardDataset dataset = datasetRepository
                .findByDashboardId(dashboard.getId())
                .orElseGet(DashboardDataset::new);
        dataset.setDashboard(dashboard);
        dataset.setDatasetName(filename);
        dataset.setColumnsJson(writeJson(parsed.columns()));
        dataset.setRowsJson(writeJson(parsed.rows()));
        dataset.setRowCount(parsed.rows().size());
        dataset.setUpdatedAt(LocalDateTime.now());

        return toResponse(datasetRepository.save(dataset));
    }

    @Transactional
    public void deleteDataset() {
        Dashboard dashboard = dashboardService.getMyDashboard();
        datasetRepository.findByDashboardId(dashboard.getId())
                .ifPresent(datasetRepository::delete);
    }

    @Transactional
    public List<ChartDataPoint> getWidgetData(Long widgetId) {
        Dashboard dashboard = dashboardService.getMyDashboard();
        DashboardDataset dataset = requireDataset(dashboard);
        WidgetConfig widget = findWidget(widgetId, dashboard.getId());
        return aggregate(widget, readColumns(dataset), readRows(dataset));
    }

    @Transactional
    public List<WidgetResult> getMyWidgetResults() {
        Dashboard dashboard = dashboardService.getMyDashboard();
        DashboardDataset dataset = requireDataset(dashboard);
        List<String> columns = readColumns(dataset);
        List<List<String>> rows = readRows(dataset);
        return widgetConfigRepository
                .findByDashboardIdOrderByPositionAscIdAsc(dashboard.getId())
                .stream()
                .map(widget -> new WidgetResult(
                        WidgetResponse.from(widget),
                        aggregate(widget, columns, rows)))
                .toList();
    }

    private ParsedDataset parseCsv(MultipartFile file) throws IOException {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {
            var records = parser.iterator();
            if (!records.hasNext()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CSV file has no header row");
            }
            List<String> columns = normalizeColumns(records.next().stream().toList());
            List<List<String>> rows = new ArrayList<>();
            while (records.hasNext()) {
                CSVRecord record = records.next();
                if (record.size() > columns.size()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "A CSV row has more values than the header");
                }
                List<String> row = new ArrayList<>(columns.size());
                for (int column = 0; column < columns.size(); column++) {
                    row.add(column < record.size() ? record.get(column) : "");
                }
                addRow(rows, row);
            }
            return new ParsedDataset(columns, rows);
        }
    }

    private ParsedDataset parseExcel(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The workbook has no worksheets");
            }
            var sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null || header.getLastCellNum() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The first worksheet has no header row");
            }
            if (sheet.getLastRowNum() > MAX_ROWS) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Datasets cannot contain more than 50,000 data rows");
            }
            int columnCount = header.getLastCellNum();
            if (columnCount > MAX_COLUMNS) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Datasets cannot have more than 100 columns");
            }

            DataFormatter formatter = new DataFormatter();
            var evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<String> columns = new ArrayList<>(columnCount);
            for (int column = 0; column < columnCount; column++) {
                columns.add(formatter.formatCellValue(header.getCell(column), evaluator));
            }
            columns = normalizeColumns(columns);

            List<List<String>> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row != null && row.getLastCellNum() > columns.size()) {
                    for (int column = columns.size(); column < row.getLastCellNum(); column++) {
                        if (!formatter.formatCellValue(row.getCell(column), evaluator).isBlank()) {
                            throw new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST, "A worksheet row has values beyond the header columns");
                        }
                    }
                }
                List<String> values = new ArrayList<>(columns.size());
                boolean hasValue = false;
                for (int column = 0; column < columns.size(); column++) {
                    String value = row == null
                            ? ""
                            : formatter.formatCellValue(row.getCell(column), evaluator);
                    values.add(value);
                    hasValue |= !value.isBlank();
                }
                if (hasValue) {
                    addRow(rows, values);
                }
            }
            return new ParsedDataset(columns, rows);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The XLSX file is invalid or cannot be read");
        }
    }

    private List<String> normalizeColumns(List<String> rawColumns) {
        if (rawColumns.isEmpty() || rawColumns.size() > MAX_COLUMNS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Datasets must have between 1 and 100 columns");
        }

        List<String> columns = new ArrayList<>(rawColumns.size());
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < rawColumns.size(); index++) {
            String column = rawColumns.get(index).trim();
            if (index == 0 && column.startsWith("\uFEFF")) {
                column = column.substring(1).trim();
            }
            if (column.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Every dataset column must have a name");
            }
            if (!seen.add(column.toLowerCase(Locale.ROOT))) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Dataset column names must be unique");
            }
            columns.add(column);
        }
        return columns;
    }

    private void addRow(List<List<String>> rows, List<String> row) {
        if (rows.size() >= MAX_ROWS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Datasets cannot contain more than 50,000 rows");
        }
        rows.add(row);
    }

    private List<ChartDataPoint> aggregate(
            WidgetConfig widget,
            List<String> columns,
            List<List<String>> rows) {
        int valueIndex = columns.indexOf(widget.getValueField());
        int groupIndex = widget.getGroupByField() == null
                ? -1
                : columns.indexOf(widget.getGroupByField());
        if (valueIndex < 0 || (widget.getGroupByField() != null && groupIndex < 0)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A saved widget uses a column that is no longer in the uploaded dataset");
        }

        Map<String, Aggregate> groups = new LinkedHashMap<>();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            String rawValue = row.get(valueIndex).trim();
            if (rawValue.isEmpty()) {
                continue;
            }

            BigDecimal value;
            if ("COUNT".equals(widget.getMetricType())) {
                value = BigDecimal.ONE;
            } else {
                try {
                    value = new BigDecimal(rawValue);
                } catch (NumberFormatException exception) {
                    throw new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "Column '" + widget.getValueField()
                                    + "' contains a non-numeric value at data row " + (rowIndex + 1));
                }
            }

            String name = groupIndex < 0
                    ? "All rows"
                    : row.get(groupIndex).isBlank() ? "(Blank)" : row.get(groupIndex);
            groups.computeIfAbsent(name, ignored -> new Aggregate()).add(value);
        }

        return groups.entrySet().stream()
                .map(entry -> new ChartDataPoint(
                        entry.getKey(),
                        toMetricValue(entry.getValue(), widget.getMetricType())))
                .toList();
    }

    private double toMetricValue(Aggregate aggregate, String metricType) {
        BigDecimal result = "AVERAGE".equals(metricType)
                ? aggregate.sum.divide(BigDecimal.valueOf(aggregate.count), MathContext.DECIMAL128)
                : aggregate.sum;
        double value = result.doubleValue();
        if (!Double.isFinite(value)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "The metric result is outside the supported numeric range");
        }
        return value;
    }

    private DashboardDataset requireDataset(Dashboard dashboard) {
        return datasetRepository.findByDashboardId(dashboard.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "Upload a dataset before requesting chart results"));
    }

    private WidgetConfig findWidget(Long widgetId, Long dashboardId) {
        return widgetConfigRepository.findById(widgetId)
                .filter(widget -> widget.getDashboard().getId().equals(dashboardId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Widget not found"));
    }

    private List<String> readColumns(DashboardDataset dataset) {
        return readJson(dataset.getColumnsJson(), COLUMNS_TYPE);
    }

    private List<List<String>> readRows(DashboardDataset dataset) {
        return readJson(dataset.getRowsJson(), ROWS_TYPE);
    }

    private DatasetResponse toResponse(DashboardDataset dataset) {
        return new DatasetResponse(dataset.getDatasetName(), readColumns(dataset), dataset.getRowCount());
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored dataset data is invalid", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not store uploaded dataset", exception);
        }
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int separator = Math.max(originalFilename.lastIndexOf('/'), originalFilename.lastIndexOf('\\'));
        return originalFilename.substring(separator + 1);
    }

    private record ParsedDataset(List<String> columns, List<List<String>> rows) {
    }

    private static class Aggregate {
        private BigDecimal sum = BigDecimal.ZERO;
        private long count;

        private void add(BigDecimal value) {
            sum = sum.add(value);
            count++;
        }
    }

    public record WidgetResult(WidgetResponse widget, List<ChartDataPoint> data) {
    }
}
