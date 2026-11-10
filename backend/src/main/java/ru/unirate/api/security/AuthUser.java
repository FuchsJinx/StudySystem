package ru.unirate.api.security;

import ru.unirate.domain.model.UserRole;

public record AuthUser(Long id, String email, UserRole role, Long studentProfileId, Long teacherProfileId) {

    public boolean isStudent() {
        return role == UserRole.STUDENT;
    }

    public boolean isTeacher() {
        return role == UserRole.TEACHER;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isStaff() {
        return isTeacher() || isAdmin();
    }
}
