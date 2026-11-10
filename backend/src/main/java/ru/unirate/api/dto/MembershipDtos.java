package ru.unirate.api.dto;

import ru.unirate.domain.model.MembershipStatus;

import java.time.Instant;

public final class MembershipDtos {

    private MembershipDtos() {
    }

    public record MembershipRequestDto(
            Long studentUserId,
            Long studentProfileId,
            String fullName,
            String email,
            Long groupId,
            String groupName,
            String facultyName,
            MembershipStatus status,
            Instant requestedAt
    ) {
    }
}
