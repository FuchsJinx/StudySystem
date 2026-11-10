package ru.unirate.application;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Human-readable dates for feed/notification messages (Russian demo). */
public final class DateFormats {

    private static final DateTimeFormatter DEADLINE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Moscow"));

    private DateFormats() {
    }

    public static String deadline(Instant instant) {
        return instant == null ? "—" : DEADLINE.format(instant);
    }
}
