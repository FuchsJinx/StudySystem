package ru.unirate.api.dto;

import java.time.Instant;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationDto(
            Long id,
            String title,
            String body,
            String kind,
            String link,
            boolean read,
            Instant createdAt
    ) {
    }

    public record UnreadCountDto(long count) {
    }
}
