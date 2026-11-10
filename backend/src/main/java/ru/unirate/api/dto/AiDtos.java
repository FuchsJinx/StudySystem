package ru.unirate.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AiDtos {

    private AiDtos() {
    }

    public record DraftCheckpointRequest(
            @NotBlank(message = "Укажите название задачи")
            @Size(max = 255, message = "Название слишком длинное")
            String title,
            @NotBlank(message = "Опишите тезисы задания")
            @Size(max = 3000, message = "Тезисы слишком длинные")
            String theses
    ) {
    }

    public record AiTextResponse(String text, String provider) {
    }

    public record PollSummaryRequest(
            @NotNull(message = "Укажите опрос")
            Long pollId
    ) {
    }

    public record RatingTipsResponse(List<String> tips, String provider) {
    }
}
