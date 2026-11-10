package ru.unirate.api.dto;

import ru.unirate.domain.model.PointCurrency;

import java.time.Instant;

public final class FeedDtos {

    private FeedDtos() {
    }

    public record AwardFeedItemDto(
            Long id,
            Long studentUserId,
            String studentName,
            String groupName,
            PointCurrency currency,
            int amount,
            String activityTitle,
            String teacherName,
            Instant createdAt
    ) {
    }

    public record PersonalFeedItemDto(
            Long id,
            String type,
            String message,
            Instant createdAt
    ) {
    }
}
