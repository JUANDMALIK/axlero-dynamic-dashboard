
package com.axlero.dashboard.service;

import com.axlero.dashboard.entity.Dashboard;
import com.axlero.dashboard.entity.Dataset;
import com.axlero.dashboard.repository.DatasetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatasetService {

    private static final int MAX_ROWS = 100_000;
    private static final int MAX_COLUMNS = 100;

    private final DatasetRepository datasetRepository;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    public DatasetService(
            DatasetRepository datasetRepository,
            DashboardService dashboardService,
            ObjectMapper objectMapper) {
        this.datasetRepository = datasetRepository;
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
    }

    @CacheEvict(cacheNames = "datasetAnalytics", key = "#p0")
    @Transactional
    public Map<String, Object> upload(
            String username, String filename, byte[] bytes) {

        if (bytes == null || bytes.length == 0) {
            throw badRequest("Choose a non-empty CSV file.");
        }

        if (filename == null ||
                !filename.toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
            throw badRequest("Only .csv files are supported.");
        }

        String csv;

        try {
            csv = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw badRequest("The CSV file must use UTF-8 encoding.");
        }

        List<List<String>> rows;

        try {
            rows = CsvParser.parse(csv);
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }

        int columnCount = rows.get(0).size();
        int dataRows = rows.size() - 1;

        if (columnCount > MAX_COLUMNS) {
            throw badRequest(
                    "CSV files may contain at most " + MAX_COLUMNS + " columns.");
        }

        if (dataRows > MAX_ROWS) {
            throw badRequest(
                    "CSV files may contain at most " + MAX_ROWS + " data rows.");
        }

        List<String> headers = rows.get(0);

        Dashboard dashboard =
                dashboardService.getOrCreateDashboard(username);

        Dataset dataset = new Dataset();

        dataset.setFilename(
                java.nio.file.Paths.get(filename).getFileName().toString());

        dataset.setCsvContent(csv);
        dataset.setRowCount(dataRows);

        try {
            dataset.setHeadersJson(
                    objectMapper.writeValueAsString(headers));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not encode CSV headers.", exception);
        }

        dataset.setDashboard(dashboard);

        Dataset saved = datasetRepository.save(dataset);

        return metadata(saved, headers);
    }

    @Transactional
    public Map<String, Object> latest(String username) {

        Dashboard dashboard =
                dashboardService.getOrCreateDashboard(username);

        Dataset dataset = datasetRepository
                .findFirstByDashboardIdOrderByUploadedAtDesc(
                        dashboard.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No CSV dataset has been uploaded yet."));

        return metadata(dataset, readHeaders(dataset));
    }

    @Cacheable(cacheNames = "datasetAnalytics", key = "#p0")
    @Transactional
    public Map<String, Object> analytics(String username) {

        Dashboard dashboard =
                dashboardService.getOrCreateDashboard(username);

        Dataset dataset = datasetRepository
                .findFirstByDashboardIdOrderByUploadedAtDesc(
                        dashboard.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Upload a CSV file before requesting analytics."));

        List<List<String>> rows;

        try {
            rows = CsvParser.parse(dataset.getCsvContent());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "The saved CSV data could not be parsed.", exception);
        }

        List<String> headers = rows.get(0);

        List<Map<String, Object>> numericColumns =
                new ArrayList<>();

        for (int column = 0; column < headers.size(); column++) {

            double sum = 0;
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            int numericCount = 0;

            for (int r = 1; r < rows.size(); r++) {

                String value = rows.get(r).get(column).trim();

                if (value.isEmpty()) {
                    continue;
                }

                try {
                    double number = Double.parseDouble(value);

                    if (!Double.isFinite(number)) {
                        continue;
                    }

                    sum += number;
                    min = Math.min(min, number);
                    max = Math.max(max, number);
                    numericCount++;

                } catch (NumberFormatException ignored) {
                    // Text values are not counted as numeric observations.
                }
            }

            if (numericCount > 0) {

                Map<String, Object> stats =
                        new LinkedHashMap<>();

                stats.put("column", headers.get(column));
                stats.put("numericCount", numericCount);
                stats.put("sum", sum);
                stats.put("average", sum / numericCount);
                stats.put("min", min);
                stats.put("max", max);

                numericColumns.add(stats);
            }
        }

        Map<String, Object> result =
                metadata(dataset, headers);

        result.put("numericColumns", numericColumns);

        int labelColumn = -1;
        for (int column = 0; column < headers.size(); column++) {
            boolean hasNumericValue = false;

            for (int r = 1; r < rows.size(); r++) {
                try {
                    double value = Double.parseDouble(rows.get(r).get(column).trim());
                    if (Double.isFinite(value)) {
                        hasNumericValue = true;
                        break;
                    }
                } catch (NumberFormatException ignored) {
                    // A non-numeric value can identify a chart category.
                }
            }

            if (!hasNumericValue) {
                labelColumn = column;
                break;
            }
        }

        List<Map<String, Object>> chartData = new ArrayList<>();

        for (int r = 1; r < rows.size(); r++) {
            Map<String, Object> chartRow = new LinkedHashMap<>();

            chartRow.put(
                    "label",
                    labelColumn >= 0
                            ? rows.get(r).get(labelColumn)
                            : "Row " + r
            );

            for (int column = 0; column < headers.size(); column++) {
                try {
                    double value = Double.parseDouble(rows.get(r).get(column).trim());

                    if (Double.isFinite(value)) {
                        chartRow.put(headers.get(column), value);
                    }
                } catch (NumberFormatException ignored) {
                    // Non-numeric cells are not chart series.
                }
            }

            chartData.add(chartRow);
        }

        result.put("chartData", chartData);
        result.put(
                "chartLabelKey",
                labelColumn >= 0 ? headers.get(labelColumn) : "label"
        );

        result.put("message", numericColumns.isEmpty()
                ? "No numeric columns were found in this dataset."
                : "Statistics calculated from numeric values in the uploaded CSV.");

        return result;
    }

    private List<String> readHeaders(Dataset dataset) {

        try {
            return objectMapper.readValue(
                    dataset.getHeadersJson(),
                    new TypeReference<List<String>>() {});

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not read saved CSV headers.", exception);
        }
    }

    private Map<String, Object> metadata(
            Dataset dataset, List<String> headers) {

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("id", dataset.getId());
        result.put("filename", dataset.getFilename());
        result.put("rowCount", dataset.getRowCount());
        result.put("columnCount", headers.size());
        result.put("headers", headers);
        result.put("uploadedAt", dataset.getUploadedAt());

        return result;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, message);
    }
}

