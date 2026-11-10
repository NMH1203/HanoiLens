package vn.edu.usth.hanoilens.shared;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every failure leaves the API as the shared {@link ErrorResponse} envelope. Exception messages from
 * frameworks or the database are never echoed back to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.status()).body(ErrorResponse.of(ex.code(), ex.getMessage(), ex.details()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorResponse.Detail> details = ex.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::toDetail)
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of("VALIDATION_ERROR", "The request contains invalid fields.", details));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        String cause = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (cause.contains("uq_journeys_one_open_per_user")) {
            return conflict("ACTIVE_VISIT_EXISTS", "An active or paused visit already exists.");
        }
        if (cause.contains("uq_users_email_lower")) {
            return conflict("EMAIL_ALREADY_REGISTERED", "This email is already registered.");
        }
        log.warn("Data integrity violation [{}]", MDC.get(RequestIdFilter.MDC_KEY));
        return conflict("CONFLICT", "The request conflicts with the current state of the resource.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("FORBIDDEN", "You are not allowed to perform this action.", List.of()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Log the type only; messages can contain request data (coordinates, tokens).
        log.error("Unhandled exception {} [{}]", ex.getClass().getName(), MDC.get(RequestIdFilter.MDC_KEY));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred.", List.of()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorResponse.Detail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.Detail(fe.getField(), String.valueOf(fe.getDefaultMessage())))
                .toList();
        return new ResponseEntity<>(
                ErrorResponse.of("VALIDATION_ERROR", "The request contains invalid fields.", details),
                headers, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return new ResponseEntity<>(ErrorResponse.of(codeFor(statusCode), messageFor(statusCode), List.of()),
                headers, statusCode);
    }

    private static ResponseEntity<ErrorResponse> conflict(String code, String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(code, message, List.of()));
    }

    private static ErrorResponse.Detail toDetail(ConstraintViolation<?> v) {
        String path = v.getPropertyPath().toString();
        return new ErrorResponse.Detail(path.substring(path.lastIndexOf('.') + 1), v.getMessage());
    }

    private static String codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 401 -> "UNAUTHENTICATED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> status.is5xxServerError() ? "INTERNAL_ERROR" : "BAD_REQUEST";
        };
    }

    private static String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "Resource not found.";
            case 405 -> "Method not allowed.";
            case 406 -> "Not acceptable.";
            case 413 -> "The request is too large.";
            case 415 -> "Unsupported media type.";
            default -> status.is5xxServerError()
                    ? "An unexpected error occurred."
                    : "The request is malformed.";
        };
    }
}
