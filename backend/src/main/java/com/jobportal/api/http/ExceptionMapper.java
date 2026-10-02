package com.jobportal.api.http;

import com.jobportal.exception.AlreadyAppliedException;
import com.jobportal.exception.DuplicateEmailException;
import com.jobportal.exception.InterviewConflictException;
import com.jobportal.exception.InvalidCredentialsException;
import com.jobportal.exception.InvalidStatusTransitionException;
import com.jobportal.exception.JobClosedException;
import com.jobportal.exception.NotFoundException;
import com.jobportal.exception.UnauthorizedException;
import com.jobportal.exception.UserBlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates exceptions into HTTP status codes and a consistent JSON error body.
 * Phase 3 equivalent: a @RestControllerAdvice with @ExceptionHandler methods.
 */
public final class ExceptionMapper {

    /** JSON error body. {@code todo} is set when the failure is an unimplemented exercise. */
    public record ErrorBody(int status, String error, String message, String todo) {
    }

    private static final Logger log = LoggerFactory.getLogger(ExceptionMapper.class);
    private static final Pattern TODO = Pattern.compile("TODO\\(#\\d+\\)");

    private ExceptionMapper() {
    }

    public static ErrorBody map(Throwable e) {
        int status = statusFor(e);
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        String todo = null;
        if (status == 501) {
            Matcher m = TODO.matcher(message);
            todo = m.find() ? m.group() : null;
            message = "This feature is not implemented yet: " + message;
        }
        if (status >= 500 && status != 501) {
            log.error("Unhandled error", e);
            message = "Internal server error"; // never leak internals to clients
        }
        return new ErrorBody(status, reason(status), message, todo);
    }

    private static int statusFor(Throwable e) {
        return switch (e) {
            case ApiException api -> api.status();
            case NotFoundException ignored -> 404;
            case InvalidCredentialsException ignored -> 401;
            case UserBlockedException ignored -> 403;
            case UnauthorizedException ignored -> 403;
            case AlreadyAppliedException ignored -> 409;
            case DuplicateEmailException ignored -> 409;
            case InterviewConflictException ignored -> 409;
            case InvalidStatusTransitionException ignored -> 409;
            case JobClosedException ignored -> 409;
            case IllegalStateException ignored -> 409;
            case IllegalArgumentException ignored -> 400;   // includes NumberFormatException
            case UnsupportedOperationException ignored -> 501;
            default -> 500;
        };
    }

    private static String reason(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 501 -> "Not Implemented";
            default -> "Internal Server Error";
        };
    }
}
