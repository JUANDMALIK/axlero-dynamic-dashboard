package com.axlero.dashboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.axlero.dashboard.dto.ChartDataPoint;
import com.axlero.dashboard.dto.DatasetResponse;
import com.axlero.dashboard.dto.WidgetResponse;
import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.DashboardDataset;
import com.axlero.dashboard.entity.Dataset;
import com.axlero.dashboard.entity.WidgetConfig;
import com.axlero.dashboard.repository.DashboardDatasetRepository;
import com.axlero.dashboard.repository.DatasetRepository;
import com.axlero.dashboard.repository.WidgetConfigRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
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
    private static final int MAX_ANALYTICS_ROWS = 100_000;
    private static final TypeReference<List<List<String>>> ROWS_TYPE =
            new TypeReference<>() { };
    private static final TypeReference<List<String>> COLUMNS_TYPE =
            new TypeReference<>() { };

    private final DashboardService dashboardService;
    private final DashboardDatasetRepository dashboardDatasetRepository;
    private final WidgetConfigRepository widgetConfigRepository;
    private final ObjectMapper objectMapper;
    private final DatasetRepository analyticsDatasetRepository;

    @Autowired
    public DatasetService(
            DashboardService dashboardService,
            DashboardDatasetRepository dashboardDatasetRepository,
            WidgetConfigRepository widgetConfigRepository,
            ObjectMapper objectMapper,
            DatasetRepository analyticsDatasetRepository) {
        this.dashboardService = dashboardService;
        this.dashboardDatasetRepository = dashboardDatasetRepository;
        this.widgetConfigRepository = widgetConfigRepository;
        this.objectMapper = objectMapper;
        this.analyticsDatasetRepository = analyticsDatasetRepository;
    }

    public DatasetService(
            DashboardService dashboardService,
            DashboardDatasetRepository dashboardDatasetRepository,
            WidgetConfigRepository widgetConfigRepository,
            ObjectMapper objectMapper) {
        this(dashboardService, dashboardDatasetRepository, widgetConfigRepository,
                objectMapper, null);
    }

    @Transactional
    public DatasetResponse getDataset() {
        Dashboard dashboard = dashboardService.getMyDashboard();
        return dashboardDatasetRepository.findByDashboardId(dashboard.getId())
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
        DashboardDataset dataset = dashboardDatasetRepository
                .findByDashboardId(dashboard.getId())
                .orElseGet(DashboardDataset::new);
        dataset.setDashboard(dashboard);
        dataset.setDatasetName(filename);
        dataset.setColumnsJson(writeJson(parsed.columns()));
        dataset.setRowsJson(writeJson(parsed.rows()));
        dataset.setRowCount(parsed.rows().size());
        dataset.setUpdatedAt(LocalDateTime.now());

        return toResponse(dashboardDatasetRepository.save(dataset));
    }

    @Transactional
    public void deleteDataset() {
        Dashboard dashboard = dashboardService.getMyDashboard();
        dashboardDatasetRepository.findByDashboardId(dashboard.getId())
                .ifPresent(dashboardDatasetRepository::delete);
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
                .filter(widget -> !"VALUE".equalsIgnoreCase(widget.getMetricType()))
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
        return dashboardDatasetRepository.findByDashboardId(dashboard.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "Upload a dataset before requesting chart results"));
    }

    private WidgetConfig findWidget(Long widgetId, Long dashboardId) {
        return widgetConfigRepository.findById(widgetId)
                .filter(widget -> !"VALUE".equalsIgnoreCase(widget.getMetricType())
                        && widget.getDashboard().getId().equals(dashboardId))
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

    @CacheEvict(cacheNames = "datasetAnalytics", key = "#p0")
    @Transactional
    public Map<String, Object> upload(String username, String filename, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choose a non-empty CSV file.");
        }
        if (filename == null
                || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only .csv files are supported.");
        }

        String csv;
        try {
            csv = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The CSV file must use UTF-8 encoding.");
        }

        List<List<String>> rows;
        try {
            rows = CsvParser.parse(csv);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        int columnCount = rows.get(0).size();
        int dataRows = rows.size() - 1;
        if (columnCount > MAX_COLUMNS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CSV files may contain at most " + MAX_COLUMNS + " columns.");
        }
        if (dataRows > MAX_ANALYTICS_ROWS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CSV files may contain at most " + MAX_ANALYTICS_ROWS + " data rows.");
        }

        Dashboard dashboard = dashboardService.getOrCreateDashboard(username);
        Dataset dataset = new Dataset();
        dataset.setFilename(safeFilename(filename));
        dataset.setCsvContent(csv);
        dataset.setRowCount(dataRows);
        dataset.setHeadersJson(writeJson(rows.get(0)));
        dataset.setDashboard(dashboard);
        Dataset saved = analyticsDatasetRepository().save(dataset);
        return analyticsMetadata(saved, rows.get(0));
    }

    @Transactional
    public Map<String, Object> latest(String username) {
        Dashboard dashboard = dashboardService.getOrCreateDashboard(username);
        Dataset dataset = analyticsDatasetRepository()
                .findFirstByDashboardIdOrderByUploadedAtDesc(dashboard.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No CSV dataset has been uploaded yet."));
        return analyticsMetadata(dataset, readHeaders(dataset));
    }

    @Cacheable(cacheNames = "datasetAnalytics", key = "#p0")
    @Transactional
    public Map<String, Object> analytics(String username) {
        Dashboard dashboard = dashboardService.getOrCreateDashboard(username);
        Dataset dataset = analyticsDatasetRepository()
                .findFirstByDashboardIdOrderByUploadedAtDesc(dashboard.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Upload a CSV file before requesting analytics."));

        List<List<String>> rows;
        try {
            rows = CsvParser.parse(dataset.getCsvContent());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("The saved CSV data could not be parsed.", exception);
        }

        List<String> headers = rows.get(0);
        List<Map<String, Object>> numericColumns = new ArrayList<>();
        for (int column = 0; column < headers.size(); column++) {
            double sum = 0;
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            int numericCount = 0;

            for (int row = 1; row < rows.size(); row++) {
                String rawValue = rows.get(row).get(column).trim();
                if (rawValue.isEmpty()) {
                    continue;
                }
                try {
                    double value = Double.parseDouble(rawValue);
                    if (Double.isFinite(value)) {
                        sum += value;
                        min = Math.min(min, value);
                        max = Math.max(max, value);
                        numericCount++;
                    }
                } catch (NumberFormatException ignored) {
                    // Non-numeric cells are excluded from numeric statistics.
                }
            }

            if (numericCount > 0) {
                Map<String, Object> statistics = new LinkedHashMap<>();
                statistics.put("column", headers.get(column));
                statistics.put("numericCount", numericCount);
                statistics.put("sum", sum);
                statistics.put("average", sum / numericCount);
                statistics.put("min", min);
                statistics.put("max", max);
                numericColumns.add(statistics);
            }
        }

        int labelColumn = firstNonNumericColumn(headers.size(), rows);
        List<Map<String, Object>> chartData = new ArrayList<>();
        for (int row = 1; row < rows.size(); row++) {
            Map<String, Object> chartRow = new LinkedHashMap<>();
            chartRow.put("label", labelColumn >= 0
                    ? rows.get(row).get(labelColumn)
                    : "Row " + row);

            for (int column = 0; column < headers.size(); column++) {
                try {
                    double value = Double.parseDouble(rows.get(row).get(column).trim());
                    if (Double.isFinite(value)) {
                        chartRow.put(headers.get(column), value);
                    }
                } catch (NumberFormatException ignored) {
                    // Non-numeric cells are not chart series.
                }
            }
            chartData.add(chartRow);
        }

        Map<String, Object> result = analyticsMetadata(dataset, headers);
        result.put("numericColumns", numericColumns);
        result.put("chartData", chartData);
        result.put("chartLabelKey", labelColumn >= 0 ? headers.get(labelColumn) : "label");
        result.put("message", numericColumns.isEmpty()
                ? "No numeric columns were found in this dataset."
                : "Statistics calculated from numeric values in the uploaded CSV.");
        return result;
    }

    private int firstNonNumericColumn(int columnCount, List<List<String>> rows) {
        for (int column = 0; column < columnCount; column++) {
            boolean hasNumericValue = false;
            for (int row = 1; row < rows.size(); row++) {
                try {
                    double value = Double.parseDouble(rows.get(row).get(column).trim());
                    if (Double.isFinite(value)) {
                        hasNumericValue = true;
                        break;
                    }
                } catch (NumberFormatException ignored) {
                    // A non-numeric value can identify a chart category.
                }
            }
            if (!hasNumericValue) {
                return column;
            }
        }
        return -1;
    }

    private List<String> readHeaders(Dataset dataset) {
        return readJson(dataset.getHeadersJson(), COLUMNS_TYPE);
    }

    private Map<String, Object> analyticsMetadata(Dataset dataset, List<String> headers) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", dataset.getId());
        result.put("filename", dataset.getFilename());
        result.put("rowCount", dataset.getRowCount());
        result.put("columnCount", headers.size());
        result.put("headers", headers);
        result.put("uploadedAt", dataset.getUploadedAt());
        return result;
    }

    private DatasetRepository analyticsDatasetRepository() {
        if (analyticsDatasetRepository == null) {
            throw new IllegalStateException("CSV analytics storage is not configured.");
        }
        return analyticsDatasetRepository;
    }
}
