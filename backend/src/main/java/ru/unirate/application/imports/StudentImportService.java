package ru.unirate.application.imports;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.unirate.api.dto.AdminDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class StudentImportService {

    private static final Logger log = LoggerFactory.getLogger(StudentImportService.class);

    private final StudentImportRowProcessor rowProcessor;

    public StudentImportService(StudentImportRowProcessor rowProcessor) {
        this.rowProcessor = rowProcessor;
    }

    public AdminDtos.ImportReport importStudents(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest(ErrorCodes.IMPORT_INVALID_FILE, "Выберите файл для импорта");
        }
        String filename = file.getOriginalFilename() == null
                ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        List<List<String>> table;
        if (filename.endsWith(".csv")) {
            table = parseCsv(file);
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            table = parseExcel(file);
        } else {
            throw ApiException.badRequest(ErrorCodes.IMPORT_INVALID_FILE,
                    "Поддерживаются файлы CSV и Excel (.xlsx / .xls)");
        }

        Parsed parsed = readRows(table);
        int created = 0;
        int skipped = 0;
        List<AdminDtos.ImportError> errors = new ArrayList<>(parsed.errors());
        for (StudentImportRow row : parsed.rows()) {
            try {
                StudentImportRowProcessor.Outcome outcome = rowProcessor.importRow(row);
                if (outcome.created()) {
                    created++;
                } else {
                    skipped++;
                    log.info("Import row {} skipped: {}", row.rowNumber(), outcome.message());
                }
            } catch (ImportRowException ex) {
                errors.add(new AdminDtos.ImportError(row.rowNumber(), ex.getMessage()));
            } catch (RuntimeException ex) {
                log.warn("Import row {} failed", row.rowNumber(), ex);
                errors.add(new AdminDtos.ImportError(row.rowNumber(),
                        "Не удалось создать студента: " + ex.getMessage()));
            }
        }
        return new AdminDtos.ImportReport(created, skipped, errors);
    }

    private Parsed readRows(List<List<String>> table) {
        if (table.isEmpty()) {
            throw ApiException.badRequest(ErrorCodes.IMPORT_INVALID_FILE, "Файл пуст");
        }
        int headerIndex = -1;
        for (int i = 0; i < table.size(); i++) {
            if (table.get(i).stream().anyMatch(cell -> cell != null && !cell.isBlank())) {
                headerIndex = i;
                break;
            }
        }
        if (headerIndex < 0) {
            throw ApiException.badRequest(ErrorCodes.IMPORT_INVALID_FILE, "Файл пуст");
        }
        Map<String, Integer> columns = new HashMap<>();
        List<String> header = table.get(headerIndex);
        for (int i = 0; i < header.size(); i++) {
            String normalized = normalizeHeader(header.get(i));
            if (!normalized.isEmpty()) {
                columns.putIfAbsent(normalized, i);
            }
        }
        List<String> missing = new ArrayList<>();
        for (String required : List.of("email", "fullname", "facultyname", "groupname")) {
            if (!columns.containsKey(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw ApiException.badRequest(ErrorCodes.IMPORT_INVALID_FILE,
                    "В файле отсутствуют обязательные колонки: " + String.join(", ", missing)
                            + ". Ожидаются: email, fullName, facultyName, groupName, passwordOptional");
        }
        Integer passwordColumn = columns.get("passwordoptional");
        if (passwordColumn == null) {
            passwordColumn = columns.get("password");
        }

        List<StudentImportRow> rows = new ArrayList<>();
        List<AdminDtos.ImportError> errors = new ArrayList<>();
        for (int i = headerIndex + 1; i < table.size(); i++) {
            List<String> cells = table.get(i);
            if (cells.stream().allMatch(cell -> cell == null || cell.isBlank())) {
                continue;
            }
            int rowNumber = i + 1;
            rows.add(new StudentImportRow(
                    rowNumber,
                    cell(cells, columns.get("email")),
                    cell(cells, columns.get("fullname")),
                    cell(cells, columns.get("facultyname")),
                    cell(cells, columns.get("groupname")),
                    passwordColumn == null ? null : cell(cells, passwordColumn)
            ));
        }
        return new Parsed(rows, errors);
    }

    private String cell(List<String> cells, Integer index) {
        if (index == null || index < 0 || index >= cells.size()) {
            return "";
        }
        String value = cells.get(index);
        return value == null ? "" : value.trim();
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace("\uFEFF", "")
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
        if (normalized.equals("фио")) {
            return "fullname";
        }
        if (normalized.equals("факультет")) {
            return "facultyname";
        }
        if (normalized.equals("группа")) {
            return "groupname";
        }
        if (normalized.equals("пароль")) {
            return "passwordoptional";
        }
        if (normalized.equals("почта")) {
            return "email";
        }
        return normalized;
    }

    private List<List<String>> parseCsv(MultipartFile file) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }
            List<List<String>> rows = new ArrayList<>();
            List<String> currentRow = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean inQuotes = false;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (inQuotes) {
                    if (c == '"') {
                        if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                            field.append('"');
                            i++;
                        } else {
                            inQuotes = false;
                        }
                    } else {
                        field.append(c);
                    }
                } else if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    currentRow.add(field.toString());
                    field.setLength(0);
                } else if (c == '\n' || c == '\r') {
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                    currentRow.add(field.toString());
                    field.setLength(0);
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                } else {
                    field.append(c);
                }
            }
            if (field.length() > 0 || !currentRow.isEmpty()) {
                currentRow.add(field.toString());
                rows.add(currentRow);
            }
            return rows;
        } catch (IOException ex) {
            throw ApiException.badRequest(ErrorCodes.IMPORT_INVALID_FILE, "Не удалось прочитать CSV-файл");
        }
    }

    private List<List<String>> parseExcel(MultipartFile file) {
        List<List<String>> rows = new ArrayList<>();
        try (InputStream input = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                int lastCell = Math.max(row.getLastCellNum(), 0);
                for (int i = 0; i < lastCell; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    cells.add(cell == null ? "" : formatter.formatCellValue(cell));
                }
                rows.add(cells);
            }
            return rows;
        } catch (IOException | RuntimeException ex) {
            throw ApiException.badRequest(ErrorCodes.IMPORT_INVALID_FILE,
                    "Не удалось прочитать Excel-файл. Проверьте формат и первый лист");
        }
    }

    private record Parsed(List<StudentImportRow> rows, List<AdminDtos.ImportError> errors) {
    }
}
