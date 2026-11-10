package ru.unirate.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterStudentRequest(
            @NotBlank(message = "Укажите email")
            @Email(message = "Некорректный email")
            @Size(max = 320, message = "Email слишком длинный")
            String email,
            @NotBlank(message = "Укажите пароль")
            @Size(min = 8, max = 72, message = "Пароль должен содержать от 8 до 72 символов")
            String password,
            @NotBlank(message = "Укажите ФИО")
            @Size(max = 255, message = "ФИО слишком длинное")
            String fullName,
            @NotNull(message = "Выберите факультет")
            Long facultyId,
            @NotNull(message = "Выберите группу")
            Long groupId
    ) {
    }

    public record RegisterTeacherRequest(
            @NotBlank(message = "Укажите email")
            @Email(message = "Некорректный email")
            @Size(max = 320, message = "Email слишком длинный")
            String email,
            @NotBlank(message = "Укажите пароль")
            @Size(min = 8, max = 72, message = "Пароль должен содержать от 8 до 72 символов")
            String password,
            @NotBlank(message = "Укажите ФИО")
            @Size(max = 255, message = "ФИО слишком длинное")
            String fullName,
            @NotNull(message = "Выберите кафедру")
            Long departmentId
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "Укажите email")
            @Email(message = "Некорректный email")
            String email,
            @NotBlank(message = "Укажите пароль")
            String password
    ) {
    }

    public record VerifyEmailRequest(
            @NotBlank(message = "Укажите токен подтверждения")
            String token
    ) {
    }

    public record AuthResponse(String token, ProfileDtos.UserMe user) {
    }
}
