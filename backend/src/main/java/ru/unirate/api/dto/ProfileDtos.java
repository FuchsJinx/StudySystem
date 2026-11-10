package ru.unirate.api.dto;

import ru.unirate.domain.model.MembershipStatus;
import ru.unirate.domain.model.PointCurrency;
import ru.unirate.domain.model.UserRole;

import java.time.Instant;
import java.util.List;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record UserMe(
            Long id,
            String email,
            String fullName,
            UserRole role,
            boolean emailVerified,
            Instant createdAt,
            StudentInfo student,
            TeacherInfo teacher
    ) {
    }

    public record StudentInfo(
            Long id,
            Long groupId,
            String groupName,
            Long facultyId,
            String facultyName,
            MembershipStatus membershipStatus,
            boolean isGroupLead,
            int academicRating,
            long bua,
            long boa
    ) {
    }

    public record TeacherInfo(
            Long id,
            Long departmentId,
            String departmentName,
            List<CatalogDtos.GroupRef> groups
    ) {
    }

    public record LedgerEntryDto(
            Long id,
            Instant date,
            PointCurrency currency,
            int amount,
            String reason,
            String actorName,
            String refType,
            Long refId
    ) {
    }

    public record ProfileCheckpointDto(
            Long id,
            String title,
            Instant deadline,
            String status,
            int cost,
            int arDebited
    ) {
    }

    public record ProfileResponse(
            Long id,
            String fullName,
            UserRole role,
            String email,
            boolean emailVerified,
            String groupName,
            String facultyName,
            String departmentName,
            Integer academicRating,
            Long bua,
            Long boa,
            MembershipStatus membershipStatus,
            Boolean isGroupLead,
            List<LedgerEntryDto> ledger,
            List<ProfileCheckpointDto> activeCheckpoints,
            List<ProfileCheckpointDto> completedCheckpoints
    ) {
    }

    public record UpdateNotificationSettingsRequest(
            boolean emailEnabled,
            boolean telegramEnabled,
            String telegramChatId
    ) {
    }

    public record NotificationSettingsDto(
            boolean emailEnabled,
            boolean telegramEnabled,
            String telegramChatId
    ) {
    }
}
