package ru.unirate.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.unirate.domain.model.MembershipStatus;
import ru.unirate.domain.model.UserRole;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record NameRequest(
            @NotBlank(message = "Укажите название")
            @Size(max = 255, message = "Название слишком длинное")
            String name
    ) {
    }

    public record GroupRequest(
            @NotBlank(message = "Укажите название группы")
            @Size(max = 64, message = "Название группы слишком длинное")
            String name,
            @NotNull(message = "Выберите факультет")
            Long facultyId
    ) {
    }

    public record FacultyAdminDto(Long id, String name, long groupsCount) {
    }

    public record GroupAdminDto(Long id, String name, Long facultyId, String facultyName) {
    }

    public record DepartmentAdminDto(Long id, String name, long teachersCount) {
    }

    public record ImportError(int row, String message) {
    }

    public record ImportReport(int created, int skipped, List<ImportError> errors) {
    }

    public record SettingDto(
            String key,
            String value,
            String title,
            String description,
            int min,
            int max,
            String unit
    ) {
    }

    public record SettingsUpdateRequest(
            @NotEmpty(message = "Нет настроек для сохранения")
            Map<String, String> values
    ) {
    }

    public record SemesterDto(
            Long id,
            String name,
            LocalDate startAt,
            LocalDate endAt,
            boolean active,
            Instant activatedAt
    ) {
    }

    public record CreateSemesterRequest(
            @NotBlank(message = "Укажите название семестра")
            @Size(max = 128, message = "Название слишком длинное")
            String name,
            @NotNull(message = "Укажите дату начала")
            LocalDate startAt,
            @NotNull(message = "Укажите дату окончания")
            LocalDate endAt
    ) {
    }

    public record SemesterActivationDto(SemesterDto semester, int affectedStudents, int totalBonus) {
    }

    public record AdminUserDto(
            Long id,
            String email,
            String fullName,
            UserRole role,
            boolean emailVerified,
            String groupName,
            String departmentName,
            MembershipStatus membershipStatus,
            Instant createdAt
    ) {
    }

    public record AdminStatsDto(
            long faculties,
            long groups,
            long departments,
            long students,
            long teachers,
            long admins,
            long pendingMemberships,
            long checkpoints,
            long activities,
            long polls,
            long awards
    ) {
    }
}
