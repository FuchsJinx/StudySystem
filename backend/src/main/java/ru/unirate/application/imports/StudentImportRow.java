package ru.unirate.application.imports;

public record StudentImportRow(
        int rowNumber,
        String email,
        String fullName,
        String facultyName,
        String groupName,
        String password
) {
}
