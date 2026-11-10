package ru.unirate.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.api.dto.CatalogDtos;
import ru.unirate.api.dto.PollDtos;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.api.security.AuthUser;
import ru.unirate.domain.model.FeedEventType;
import ru.unirate.domain.model.MembershipStatus;
import ru.unirate.domain.model.PollQuestionType;
import ru.unirate.domain.model.UserRole;
import ru.unirate.infrastructure.persistence.entity.Poll;
import ru.unirate.infrastructure.persistence.entity.PollOption;
import ru.unirate.infrastructure.persistence.entity.PollQuestion;
import ru.unirate.infrastructure.persistence.entity.PollResponse;
import ru.unirate.infrastructure.persistence.entity.StudentGroup;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.entity.TeacherProfile;
import ru.unirate.infrastructure.persistence.repo.PollRepository;
import ru.unirate.infrastructure.persistence.repo.PollResponseRepository;
import ru.unirate.infrastructure.persistence.repo.StudentGroupRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.TeacherProfileRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PollService {

    private final PollRepository pollRepository;
    private final PollResponseRepository responseRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentGroupRepository groupRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final ProfileService profileService;
    private final StudentAccessService studentAccessService;
    private final EventService eventService;

    public PollService(PollRepository pollRepository,
                       PollResponseRepository responseRepository,
                       StudentProfileRepository studentProfileRepository,
                       StudentGroupRepository groupRepository,
                       TeacherProfileRepository teacherProfileRepository,
                       ProfileService profileService,
                       StudentAccessService studentAccessService,
                       EventService eventService) {
        this.pollRepository = pollRepository;
        this.responseRepository = responseRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.groupRepository = groupRepository;
        this.teacherProfileRepository = teacherProfileRepository;
        this.profileService = profileService;
        this.studentAccessService = studentAccessService;
        this.eventService = eventService;
    }

    @Transactional
    public PollDtos.PollSummaryDto create(AuthUser user, PollDtos.CreatePollRequest request) {
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        if (!request.endsAt().isAfter(Instant.now())) {
            throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED,
                    "Дата окончания опроса должна быть в будущем");
        }
        List<StudentGroup> groups = resolveGroups(request.targetGroupIds());

        Poll poll = new Poll();
        poll.setTitle(request.title().trim());
        poll.setDescription(request.description() == null ? "" : request.description().trim());
        poll.setEndsAt(request.endsAt());
        poll.setAnonymous(request.isAnonymous());
        poll.setCreatedBy(teacher);
        poll.setGroups(new LinkedHashSet<>(groups));

        int questionOrder = 0;
        for (PollDtos.QuestionInput input : request.questions()) {
            PollQuestion question = new PollQuestion();
            question.setPoll(poll);
            question.setText(input.text().trim());
            question.setType(input.type());
            question.setSortOrder(questionOrder++);
            List<String> optionTexts = input.options().stream()
                    .map(String::trim)
                    .filter(text -> !text.isEmpty())
                    .toList();
            if (optionTexts.size() < 2) {
                throw ApiException.badRequest(ErrorCodes.VALIDATION_FAILED,
                        "В вопросе «" + input.text() + "» нужно минимум два варианта ответа");
            }
            int optionOrder = 0;
            for (String optionText : optionTexts) {
                PollOption option = new PollOption();
                option.setQuestion(question);
                option.setText(optionText);
                option.setSortOrder(optionOrder++);
                question.getOptions().add(option);
            }
            poll.getQuestions().add(question);
        }
        pollRepository.save(poll);

        teacher.getGroups().addAll(groups);
        teacherProfileRepository.save(teacher);

        for (StudentGroup group : groups) {
            List<StudentProfile> students = studentProfileRepository
                    .findAllByGroupIdAndMembershipStatus(group.getId(), MembershipStatus.CONFIRMED);
            for (StudentProfile student : students) {
                String message = "Новый опрос для вашей группы: «" + poll.getTitle() + "»";
                eventService.feedAndNotify(student.getUser(), FeedEventType.POLL_CREATED, message,
                        "Новый опрос", message + ". Проголосуйте до окончания опроса.", "/polls");
            }
        }
        return toSummary(poll, null);
    }

    @Transactional(readOnly = true)
    public List<PollDtos.PollSummaryDto> list(AuthUser user) {
        if (user.role() == UserRole.STUDENT) {
            StudentProfile student = studentAccessService.requireStudent(user);
            return pollRepository.findAllByGroupsIdOrderByEndsAtDesc(student.getGroup().getId()).stream()
                    .map(poll -> toSummary(poll, student))
                    .toList();
        }
        if (user.role() == UserRole.ADMIN) {
            return pollRepository.findAllByOrderByCreatedAtDesc().stream()
                    .map(poll -> toSummary(poll, null))
                    .toList();
        }
        TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
        return pollRepository.findByCreatedByIdOrderByCreatedAtDesc(teacher.getId()).stream()
                .map(poll -> toSummary(poll, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public PollDtos.PollDetailDto detail(AuthUser user, Long pollId) {
        Poll poll = requirePoll(pollId);
        if (user.role() == UserRole.STUDENT) {
            StudentProfile student = studentAccessService.requireStudent(user);
            requireTargeted(poll, student);
            boolean voted = responseRepository.existsByPollIdAndStudentProfileId(poll.getId(), student.getId());
            boolean ended = poll.isEnded(Instant.now());
            boolean canVote = student.isConfirmed() && student.getUser().isEmailVerified() && !voted && !ended;
            List<PollDtos.QuestionResultDto> results = voted || ended ? aggregate(poll) : null;
            return buildDetail(poll, voted, canVote, results, List.of());
        }

        if (user.role() == UserRole.TEACHER) {
            TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
            requireOwner(poll, teacher);
        }
        List<PollDtos.QuestionResultDto> results = aggregate(poll);
        List<PollDtos.VoterDto> voters = voterDetails(poll);
        return buildDetail(poll, false, false, results, voters);
    }

    @Transactional(readOnly = true)
    public PollDtos.PollResultsDto results(AuthUser user, Long pollId) {
        Poll poll = requirePoll(pollId);
        boolean includeVoters;
        if (user.role() == UserRole.STUDENT) {
            StudentProfile student = studentAccessService.requireStudent(user);
            requireTargeted(poll, student);
            boolean voted = responseRepository.existsByPollIdAndStudentProfileId(poll.getId(), student.getId());
            boolean ended = poll.isEnded(Instant.now());
            if (!voted && !ended) {
                throw ApiException.forbidden(ErrorCodes.RESULTS_NOT_AVAILABLE,
                        "Результаты станут доступны после голосования или завершения опроса");
            }
            includeVoters = false;
        } else if (user.role() == UserRole.TEACHER) {
            TeacherProfile teacher = profileService.requireTeacherProfile(user.id());
            requireOwner(poll, teacher);
            includeVoters = true;
        } else {
            includeVoters = true;
        }
        return new PollDtos.PollResultsDto(
                poll.getId(),
                poll.getTitle(),
                status(poll),
                poll.isAnonymous(),
                (int) responseRepository.countByPollId(poll.getId()),
                aggregate(poll),
                includeVoters ? voterDetails(poll) : List.of()
        );
    }

    @Transactional
    public PollDtos.PollResultsDto vote(AuthUser user, Long pollId, PollDtos.VoteRequest request) {
        StudentProfile student = studentAccessService.requireActiveStudent(user);
        Poll poll = requirePoll(pollId);
        requireTargeted(poll, student);
        if (poll.isEnded(Instant.now())) {
            throw ApiException.badRequest(ErrorCodes.POLL_ENDED, "Голосование по этому опросу завершено");
        }
        if (responseRepository.existsByPollIdAndStudentProfileId(poll.getId(), student.getId())) {
            throw ApiException.conflict(ErrorCodes.ALREADY_VOTED, "Вы уже голосовали в этом опросе");
        }

        Map<Long, PollDtos.VoteAnswer> answersByQuestion = new LinkedHashMap<>();
        for (PollDtos.VoteAnswer answer : request.answers()) {
            if (answersByQuestion.putIfAbsent(answer.questionId(), answer) != null) {
                throw ApiException.badRequest(ErrorCodes.VOTE_INVALID, "Вопрос указан дважды");
            }
        }
        if (answersByQuestion.size() != poll.getQuestions().size()) {
            throw ApiException.badRequest(ErrorCodes.VOTE_INVALID, "Ответьте на все вопросы опроса");
        }

        Set<PollOption> selectedOptions = new LinkedHashSet<>();
        Map<Long, PollQuestion> questions = new HashMap<>();
        for (PollQuestion question : poll.getQuestions()) {
            questions.put(question.getId(), question);
            PollDtos.VoteAnswer answer = answersByQuestion.get(question.getId());
            if (answer == null) {
                throw ApiException.badRequest(ErrorCodes.VOTE_INVALID, "Ответьте на все вопросы опроса");
            }
            Set<Long> optionIds = new LinkedHashSet<>(answer.optionIds());
            if (question.getType() == PollQuestionType.SINGLE && optionIds.size() != 1) {
                throw ApiException.badRequest(ErrorCodes.VOTE_INVALID,
                        "В вопросе «" + question.getText() + "» можно выбрать только один вариант");
            }
            Map<Long, PollOption> questionOptions = new HashMap<>();
            question.getOptions().forEach(option -> questionOptions.put(option.getId(), option));
            for (Long optionId : optionIds) {
                PollOption option = questionOptions.get(optionId);
                if (option == null) {
                    throw ApiException.badRequest(ErrorCodes.VOTE_INVALID,
                            "Выбран недопустимый вариант ответа");
                }
                selectedOptions.add(option);
            }
        }
        for (Long questionId : answersByQuestion.keySet()) {
            if (!questions.containsKey(questionId)) {
                throw ApiException.badRequest(ErrorCodes.VOTE_INVALID, "Неизвестный вопрос опроса");
            }
        }

        PollResponse response = new PollResponse();
        response.setPoll(poll);
        response.setStudentProfile(student);
        response.setSelectedOptions(selectedOptions);
        try {
            responseRepository.saveAndFlush(response);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict(ErrorCodes.ALREADY_VOTED, "Вы уже голосовали в этом опросе");
        }
        return new PollDtos.PollResultsDto(
                poll.getId(),
                poll.getTitle(),
                status(poll),
                poll.isAnonymous(),
                (int) responseRepository.countByPollId(poll.getId()),
                aggregate(poll),
                List.of()
        );
    }

    private List<PollDtos.QuestionResultDto> aggregate(Poll poll) {
        List<PollResponse> responses = responseRepository.findByPollId(poll.getId());
        int totalResponses = responses.size();
        List<PollDtos.QuestionResultDto> results = new ArrayList<>();
        for (PollQuestion question : poll.getQuestions()) {
            List<PollDtos.OptionResultDto> optionResults = new ArrayList<>();
            for (PollOption option : question.getOptions()) {
                int count = 0;
                for (PollResponse response : responses) {
                    if (response.getSelectedOptions().stream()
                            .anyMatch(selected -> selected.getId().equals(option.getId()))) {
                        count++;
                    }
                }
                double percent = totalResponses == 0 ? 0.0
                        : Math.round(count * 1000.0 / totalResponses) / 10.0;
                optionResults.add(new PollDtos.OptionResultDto(option.getId(), option.getText(), count, percent));
            }
            results.add(new PollDtos.QuestionResultDto(
                    question.getId(), question.getText(), question.getType(), totalResponses, optionResults));
        }
        return results;
    }

    private List<PollDtos.VoterDto> voterDetails(Poll poll) {
        if (poll.isAnonymous()) {
            return List.of();
        }
        List<PollResponse> responses = responseRepository.findByPollId(poll.getId());
        List<PollDtos.VoterDto> voters = new ArrayList<>();
        for (PollResponse response : responses) {
            List<PollDtos.VoterAnswerDto> answers = new ArrayList<>();
            for (PollQuestion question : poll.getQuestions()) {
                List<String> selected = response.getSelectedOptions().stream()
                        .filter(option -> option.getQuestion().getId().equals(question.getId()))
                        .sorted(Comparator.comparing(PollOption::getSortOrder))
                        .map(PollOption::getText)
                        .toList();
                answers.add(new PollDtos.VoterAnswerDto(question.getId(), selected));
            }
            voters.add(new PollDtos.VoterDto(
                    response.getStudentProfile().getUser().getFullName(),
                    response.getStudentProfile().getGroup().getName(),
                    answers
            ));
        }
        voters.sort(Comparator.comparing(PollDtos.VoterDto::studentName));
        return voters;
    }

    private PollDtos.PollDetailDto buildDetail(Poll poll, boolean hasVoted, boolean canVote,
                                                List<PollDtos.QuestionResultDto> results,
                                                List<PollDtos.VoterDto> voters) {
        List<PollDtos.QuestionDto> questions = poll.getQuestions().stream()
                .map(question -> new PollDtos.QuestionDto(
                        question.getId(),
                        question.getText(),
                        question.getType(),
                        question.getOptions().stream()
                                .sorted(Comparator.comparing(PollOption::getSortOrder))
                                .map(option -> new PollDtos.OptionDto(option.getId(), option.getText()))
                                .toList()
                ))
                .toList();
        return new PollDtos.PollDetailDto(
                poll.getId(),
                poll.getTitle(),
                poll.getDescription(),
                poll.getEndsAt(),
                poll.isAnonymous(),
                status(poll),
                hasVoted,
                canVote,
                poll.getGroups().stream()
                        .map(group -> new CatalogDtos.GroupRef(group.getId(), group.getName()))
                        .toList(),
                poll.getCreatedAt(),
                poll.getCreatedBy().getUser().getFullName(),
                questions,
                results,
                voters
        );
    }

    private PollDtos.PollSummaryDto toSummary(Poll poll, StudentProfile student) {
        boolean hasVoted = student != null && responseRepository
                .existsByPollIdAndStudentProfileId(poll.getId(), student.getId());
        return new PollDtos.PollSummaryDto(
                poll.getId(),
                poll.getTitle(),
                poll.getEndsAt(),
                poll.isAnonymous(),
                status(poll),
                hasVoted,
                poll.getQuestions().size(),
                poll.getCreatedAt(),
                poll.getGroups().stream()
                        .map(group -> new CatalogDtos.GroupRef(group.getId(), group.getName()))
                        .toList()
        );
    }

    private String status(Poll poll) {
        return poll.isEnded(Instant.now()) ? "ENDED" : "ACTIVE";
    }

    private Poll requirePoll(Long pollId) {
        return pollRepository.findWithDetailsById(pollId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.POLL_NOT_FOUND, "Опрос не найден"));
    }

    private void requireOwner(Poll poll, TeacherProfile teacher) {
        if (!poll.getCreatedBy().getId().equals(teacher.getId())) {
            throw ApiException.forbidden(ErrorCodes.ACCESS_DENIED, "Этот опрос создан другим преподавателем");
        }
    }

    private void requireTargeted(Poll poll, StudentProfile student) {
        boolean targeted = poll.getGroups().stream()
                .anyMatch(group -> group.getId().equals(student.getGroup().getId()));
        if (!targeted) {
            throw ApiException.forbidden(ErrorCodes.GROUP_NOT_TARGETED,
                    "Опрос не предназначен для вашей группы");
        }
    }

    private List<StudentGroup> resolveGroups(List<Long> groupIds) {
        List<StudentGroup> groups = new ArrayList<>();
        for (Long groupId : new LinkedHashSet<>(groupIds)) {
            groups.add(groupRepository.findById(groupId)
                    .orElseThrow(() -> ApiException.notFound(ErrorCodes.GROUP_NOT_FOUND,
                            "Группа не найдена: " + groupId)));
        }
        return groups;
    }
}
