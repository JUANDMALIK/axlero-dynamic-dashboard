package com.axlero.dashboard.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CsvParser {

    private CsvParser() {}

    public static List<List<String>> parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("The CSV file is empty.");
        }

        String text = input.startsWith("\uFEFF") ? input.substring(1) : input;
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closedQuote = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closedQuote = true;
                    }
                } else {
                    field.append(c);
                }
                continue;
            }

            if (closedQuote && c != ',' && c != '\r' && c != '\n'
                    && c != ' ' && c != '\t') {
                throw new IllegalArgumentException(
                        "Unexpected character after a quoted CSV value.");
            }

            if (c == '"' && field.length() == 0 && !closedQuote) {
                quoted = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
                closedQuote = false;
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < text.length()
                        && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                field.setLength(0);
                closedQuote = false;

                if (!(row.size() == 1 && row.get(0).isEmpty())) {
                    rows.add(row);
                }
                row = new ArrayList<>();
            } else if (!closedQuote) {
                field.append(c);
            }
        }

        if (quoted) {
            throw new IllegalArgumentException(
                    "The CSV file contains an unclosed quoted value.");
        }

        if (field.length() > 0 || !row.isEmpty() || closedQuote) {
            row.add(field.toString());
            rows.add(row);
        }

        if (rows.size() < 2) {
            throw new IllegalArgumentException(
                    "The CSV must contain a header and at least one data row.");
        }

        List<String> headers = new ArrayList<>();
        Set<String> uniqueHeaders = new HashSet<>();

        for (String header : rows.get(0)) {
            String normalized = header.trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(
                        "CSV headers cannot be blank.");
            }
            if (!uniqueHeaders.add(normalized.toLowerCase(java.util.Locale.ROOT))) {
                throw new IllegalArgumentException(
                        "CSV headers must be unique: " + normalized);
            }
            headers.add(normalized);
        }

        for (int i = 1; i < rows.size(); i++) {
            if (rows.get(i).size() != headers.size()) {
                throw new IllegalArgumentException(
                        "CSV row " + (i + 1) + " has "
                        + rows.get(i).size() + " columns; expected "
                        + headers.size() + ".");
            }
        }

        return rows;
    }
}
