package ru.usernamedrew.edutaskcommon.event.judge;

public enum JudgeSubmissionStatus {
    QUEUED,
    PROCESSING,
    ACCEPTED,
    WRONG_ANSWER,
    COMPILATION_ERROR,
    RUNTIME_ERROR,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    FAILED;

    public boolean isTerminal() {
        return switch (this) {
            case QUEUED, PROCESSING -> false;
            case ACCEPTED,
                 WRONG_ANSWER,
                 COMPILATION_ERROR,
                 RUNTIME_ERROR,
                 TIME_LIMIT_EXCEEDED,
                 MEMORY_LIMIT_EXCEEDED,
                 FAILED -> true;
        };
    }
}
