package vn.edu.usth.hanoilens.shared;

import java.util.List;
import org.slf4j.MDC;

/** Error envelope shared by every endpoint (docs/API.md §2). Never carries stack traces or internal details. */
public record ErrorResponse(Error error) {

    public record Error(String code, String message, List<Detail> details, String requestId) {
    }

    public record Detail(String field, String reason) {
    }

    public static ErrorResponse of(String code, String message, List<Detail> details) {
        return new ErrorResponse(new Error(code, message, details == null ? List.of() : details,
                MDC.get(RequestIdFilter.MDC_KEY)));
    }
}
