package vn.edu.usth.hanoilens.shared;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Business/validation failure that maps 1:1 to an HTTP status and an API error code. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<ErrorResponse.Detail> details;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }

    public ApiException(HttpStatus status, String code, String message, List<ErrorResponse.Detail> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public List<ErrorResponse.Detail> details() {
        return details;
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", message);
    }

    /** 404 is also used when a private resource exists but belongs to someone else (BOLA/IDOR). */
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException unprocessable(String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }

    public static ApiException invalidField(String field, String reason) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR",
                "The request contains invalid fields.", List.of(new ErrorResponse.Detail(field, reason)));
    }
}
