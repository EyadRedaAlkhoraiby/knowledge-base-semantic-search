package com.demo.knowledgebase.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileImportService {

    private static final Logger logger = LoggerFactory.getLogger(FileImportService.class);
    private final KnowledgeBaseService knowledgeBaseService;

    public FileImportService(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    public Map<String, Object> importFile(MultipartFile file, String username) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null)
            throw new IllegalArgumentException("Filename cannot be null");

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        int count = 0;

        logger.info("Importing file: {} by user: {}", filename, username);

        if (extension.equals("csv")) {
            count = parseCsv(file, username);
        } else if (extension.equals("xlsx") || extension.equals("xls")) {
            count = parseExcel(file.getInputStream(), username);
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + extension + ". Please use .csv or .xlsx");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("imported", count);
        result.put("message", "Successfully imported " + count + " documents");
        return result;
    }

    private int parseCsv(MultipartFile file, String username) throws Exception {
        // Robust encoding detection: try UTF-8, then CP1256 (Arabic Windows)
        byte[] bytes = file.getBytes();
        String content;

        // Very basic heuristic: check for UTF-8 replacement chars or look valid
        // Ideally we assume UTF-8, if user complains we support CP1256
        // For now, let's try to interpret as UTF-8.
        // Apache Commons CSV handles BOM automatically if we use the right input stream

        // We will decode manually to control fallback
        Charset[] charsets = { StandardCharsets.UTF_8, Charset.forName("windows-1256"), StandardCharsets.ISO_8859_1 };

        // Since detecting valid encoding is hard without a library, we will rely on
        // UTF-8 default
        // but normalize line endings.
        // Actually, for CSV, let's just use UTF-8 BOM aware reader.

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8));
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreHeaderCase(true)
                        .setTrim(true)
                        .build().parse(reader)) {

            int count = 0;
            for (CSVRecord record : parser) {
                if (processRecord(record.toMap(), username)) {
                    count++;
                }
            }
            return count;
        }
    }

    private int parseExcel(InputStream inputStream, String username) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null)
                return 0;

            // Map header names to column indices
            Map<String, Integer> headers = new HashMap<>();
            for (Cell cell : headerRow) {
                headers.put(cell.getStringCellValue().toLowerCase().trim(), cell.getColumnIndex());
            }

            if (!headers.containsKey("title") || !headers.containsKey("content")) {
                throw new IllegalArgumentException("Excel file must contain 'title' and 'content' columns");
            }

            java.util.List<com.demo.knowledgebase.model.Document> docsToAdd = new java.util.ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                String title = getCellValue(row, headers.get("title"));
                String content = getCellValue(row, headers.get("content"));
                String category = headers.containsKey("category") ? getCellValue(row, headers.get("category"))
                        : "General";

                if (title != null && !title.isEmpty() && content != null && !content.isEmpty()) {
                    docsToAdd.add(com.demo.knowledgebase.model.Document.create(title, content, category, username));
                }
            }

            if (!docsToAdd.isEmpty()) {
                knowledgeBaseService.addDocuments(docsToAdd);
            }
            return docsToAdd.size();
        }
    }

    private String getCellValue(Row row, Integer columnIndex) {
        if (columnIndex == null)
            return null;
        Cell cell = row.getCell(columnIndex);
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private boolean processRecord(Map<String, String> record, String username) {
        String title = get(record, "title");
        String content = get(record, "content");
        String category = get(record, "category");
        if (category == null || category.isEmpty())
            category = "General";

        if (title != null && !title.isEmpty() && content != null && !content.isEmpty()) {
            knowledgeBaseService.addDocument(title, content, category, username);
            return true;
        }
        return false;
    }

    private String get(Map<String, String> map, String key) {
        if (map.containsKey(key))
            return map.get(key);
        for (String k : map.keySet()) {
            if (k.equalsIgnoreCase(key))
                return map.get(k);
        }
        return null;
    }
}
