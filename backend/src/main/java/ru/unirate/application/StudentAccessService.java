package ru.unirate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.api.security.AuthUser;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;

/** Common student access invariants: email verification + confirmed group (spec 13). */
@Service
public class StudentAccessService {

    private final StudentProfileRepository studentProfileRepository;

    public StudentAccessService(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    @Transactional(readOnly = true)
    public StudentProfile requireStudent(AuthUser user) {
        return studentProfileRepository.findByUserId(user.id())
                .orElseThrow(() -> ApiException.forbidden(ErrorCodes.ACCESS_DENIED,
                        "Профиль студента не найден"));
    }

    /** Mutations are blocked until both email and group membership are confirmed. */
    @Transactional(readOnly = true)
    public StudentProfile requireActiveStudent(AuthUser user) {
        StudentProfile profile = requireStudent(user);
        if (!profile.getUser().isEmailVerified()) {
            throw ApiException.forbidden(ErrorCodes.EMAIL_NOT_VERIFIED,
                    "Подтвердите email, чтобы выполнить это действие");
        }
        if (!profile.isConfirmed()) {
            throw ApiException.forbidden(ErrorCodes.GROUP_NOT_CONFIRMED,
                    "Принадлежность к группе ещё не подтверждена");
        }
        return profile;
    }

    @Transactional(readOnly = true)
    public StudentProfile requireConfirmedStudent(AuthUser user) {
        StudentProfile profile = requireStudent(user);
        if (!profile.isConfirmed()) {
            throw ApiException.forbidden(ErrorCodes.GROUP_NOT_CONFIRMED,
                    "Принадлежность к группе ещё не подтверждена");
        }
        return profile;
    }
}
