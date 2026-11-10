package ru.unirate.api.dto;

import java.util.List;

public final class CommonDtos {

    private CommonDtos() {
    }

    public record PageResponse<T>(List<T> items, Long nextCursor) {
    }

    public record MessageResponse(String message) {
    }
}
