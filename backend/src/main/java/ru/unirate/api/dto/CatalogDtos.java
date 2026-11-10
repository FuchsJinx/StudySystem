package ru.unirate.api.dto;

import java.util.List;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record FacultyDto(Long id, String name) {
    }

    public record GroupDto(Long id, String name, Long facultyId, String facultyName) {
    }

    public record DepartmentDto(Long id, String name) {
    }

    public record GroupRef(Long id, String name) {
    }

    public record CatalogResponse(List<FacultyDto> faculties, List<GroupDto> groups,
                                  List<DepartmentDto> departments) {
    }
}
