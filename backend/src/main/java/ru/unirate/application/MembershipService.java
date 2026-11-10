package ru.unirate.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.dto.MembershipDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.api.security.AuthUser;
import ru.unirate.domain.model.FeedEventType;
import ru.unirate.domain.model.MembershipStatus;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.TeacherProfile;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.TeacherProfileRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@Service
public class MembershipService {

    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final ProfileService profileService;
    private final CheckpointAssignmentService assignmentService;
    private final EventService eventService;

    public MembershipService(StudentProfileRepository studentProfileRepository,
                             TeacherProfileRepository teacherProfileRepository,
                             ProfileService profileService,
                             CheckpointAssignmentService assignmentService,
                             EventService eventService) {
        this.studentProfileRepository = studentProfileRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.profileService = profileService;
        this.assignmentService = assignmentService;
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public List<MembershipDtos.MembershipRequestDto> pendingRequests(AuthUser viewer) {
        List<StudentProfile> pending;
        if (viewer.isAdmin()) {
            pending = studentProfileRepository.findAllByMembershipStatus(MembershipStatus.PENDING);
        } else if (viewer.isTeacher()) {
            TeacherProfile teacher = profileService.requireTeacherProfile(viewer.id());
            List<Long> groupIds = teacher.getGroups().stream().map(group -> group.getId()).toList();
            pending = groupIds.isEmpty() ? List.of()
                    : studentProfileRepository.findAllByGroupIdInAndMembershipStatus(groupIds, MembershipStatus.PENDING);
        } else {
            StudentProfile own = profileService.requireStudentProfile(viewer.id());
            if (!own.isGroupLead()) {
                throw ApiException.forbidden(ErrorCodes.ACCESS_DENIED,
                        "Заявки доступны преподавателям и старостам групп");
            }
            pending = studentProfileRepository.findAllByGroupIdAndMembershipStatus(
                    own.getGroup().getId(), MembershipStatus.PENDING);
        }
        List<MembershipDtos.MembershipRequestDto> result = new ArrayList<>();
        for (StudentProfile profile : pending) {
            result.add(toDto(profile));
        }
        result.sort(Comparator.comparing(MembershipDtos.MembershipRequestDto::requestedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    @Transactional
    public MembershipDtos.MembershipRequestDto confirm(AuthUser viewer, Long studentUserId) {
        StudentProfile profile = requireTarget(studentUserId);
        authorizeMembershipChange(viewer, profile);
        if (profile.getMembershipStatus() == MembershipStatus.CONFIRMED) {
            return toDto(profile);
        }
        profile.setMembershipStatus(MembershipStatus.CONFIRMED);
        profile.setMembershipConfirmedBy(profileService.requireUser(viewer.id()));
        profile.setMembershipConfirmedAt(Instant.now());
        studentProfileRepository.save(profile);
        assignmentService.assignUpcomingToStudent(profile);

        String message = "Ваша принадлежность к группе «" + profile.getGroup().getName() + "» подтверждена";
        eventService.feedAndNotify(profile.getUser(), FeedEventType.MEMBERSHIP_CONFIRMED,
                message, "Группа подтверждена", message + ". Теперь вам доступны задания и активности.",
                "/profile");
        return toDto(profile);
    }

    @Transactional
    public MembershipDtos.MembershipRequestDto reject(AuthUser viewer, Long studentUserId) {
        StudentProfile profile = requireTarget(studentUserId);
        authorizeMembershipChange(viewer, profile);
        profile.setMembershipStatus(MembershipStatus.REJECTED);
        profile.setMembershipConfirmedBy(profileService.requireUser(viewer.id()));
        profile.setMembershipConfirmedAt(Instant.now());
        studentProfileRepository.save(profile);

        String message = "Заявка на принадлежность к группе «" + profile.getGroup().getName() + "» отклонена";
        eventService.feedAndNotify(profile.getUser(), FeedEventType.MEMBERSHIP_REJECTED,
                message, "Заявка отклонена",
                message + ". Обратитесь к преподавателю или старосте группы.", "/profile");
        return toDto(profile);
    }

    private StudentProfile requireTarget(Long studentUserId) {
        StudentProfile profile = studentProfileRepository.findByUserId(studentUserId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.NOT_FOUND, "Студент не найден"));
        if (profile.getUser().getRole() != ru.unirate.domain.model.UserRole.STUDENT) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED, "Пользователь не является студентом");
        }
        return profile;
    }

    private void authorizeMembershipChange(AuthUser viewer, StudentProfile target) {
        if (viewer.isAdmin()) {
            return;
        }
        if (viewer.isTeacher()) {
            TeacherProfile teacher = profileService.requireTeacherProfile(viewer.id());
            if (teacher.worksWith(target.getGroup().getId())) {
                return;
            }
            throw ApiException.forbidden(ErrorCodes.ACCESS_DENIED,
                    "Вы не работаете с группой этого студента");
        }
        StudentProfile own = profileService.requireStudentProfile(viewer.id());
        if (own.isGroupLead() && own.getGroup().getId().equals(target.getGroup().getId())) {
            return;
        }
        throw ApiException.forbidden(ErrorCodes.ACCESS_DENIED,
                "Подтверждать одногруппников может только староста");
    }

    private MembershipDtos.MembershipRequestDto toDto(StudentProfile profile) {
        return new MembershipDtos.MembershipRequestDto(
                profile.getUser().getId(),
                profile.getId(),
                profile.getUser().getFullName(),
                profile.getUser().getEmail(),
                profile.getGroup().getId(),
                profile.getGroup().getName(),
                profile.getGroup().getFaculty().getName(),
                profile.getMembershipStatus(),
                profile.getEnrolledAt()
        );
    }
}
