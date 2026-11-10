package ru.unirate.api.error;

/** Stable error codes used by the frontend and tests. */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String EMAIL_ALREADY_USED = "EMAIL_ALREADY_USED";
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    public static final String GROUP_NOT_CONFIRMED = "GROUP_NOT_CONFIRMED";
    public static final String EMAIL_VERIFICATION_INVALID = "EMAIL_VERIFICATION_INVALID";
    public static final String EMAIL_ALREADY_VERIFIED = "EMAIL_ALREADY_VERIFIED";
    public static final String GROUP_NOT_FOUND = "GROUP_NOT_FOUND";
    public static final String FACULTY_NOT_FOUND = "FACULTY_NOT_FOUND";
    public static final String DEPARTMENT_NOT_FOUND = "DEPARTMENT_NOT_FOUND";
    public static final String MEMBERSHIP_NOT_PENDING = "MEMBERSHIP_NOT_PENDING";
    public static final String CHECKPOINT_NOT_FOUND = "CHECKPOINT_NOT_FOUND";
    public static final String SUBMISSION_NOT_FOUND = "SUBMISSION_NOT_FOUND";
    public static final String SUBMISSION_EMPTY = "SUBMISSION_EMPTY";
    public static final String SUBMISSION_INVALID_STATUS = "SUBMISSION_INVALID_STATUS";
    public static final String FILE_INVALID_TYPE = "FILE_INVALID_TYPE";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String ACTIVITY_NOT_FOUND = "ACTIVITY_NOT_FOUND";
    public static final String GROUP_NOT_TARGETED = "GROUP_NOT_TARGETED";
    public static final String ALREADY_ENROLLED = "ALREADY_ENROLLED";
    public static final String NOT_ENROLLED = "NOT_ENROLLED";
    public static final String AWARD_ALREADY_EXISTS = "AWARD_ALREADY_EXISTS";
    public static final String BUDGET_EXCEEDED = "BUDGET_EXCEEDED";
    public static final String INVALID_LEVEL = "INVALID_LEVEL";
    public static final String POLL_NOT_FOUND = "POLL_NOT_FOUND";
    public static final String POLL_ENDED = "POLL_ENDED";
    public static final String ALREADY_VOTED = "ALREADY_VOTED";
    public static final String VOTE_INVALID = "VOTE_INVALID";
    public static final String RESULTS_NOT_AVAILABLE = "RESULTS_NOT_AVAILABLE";
    public static final String SETTING_INVALID = "SETTING_INVALID";
    public static final String SEMESTER_NOT_FOUND = "SEMESTER_NOT_FOUND";
    public static final String SEMESTER_ALREADY_ACTIVATED = "SEMESTER_ALREADY_ACTIVATED";
    public static final String IMPORT_INVALID_FILE = "IMPORT_INVALID_FILE";
    public static final String AI_UNAVAILABLE = "AI_UNAVAILABLE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}
