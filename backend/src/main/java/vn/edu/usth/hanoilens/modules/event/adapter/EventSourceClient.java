package vn.edu.usth.hanoilens.modules.event.adapter;

import java.net.URI;

/** Port to the outside world for the ingestion job (replaced by a fake in tests). */
public interface EventSourceClient {

    /** Failure with a short machine-readable code that is safe to store and show to an administrator. */
    class FetchException extends RuntimeException {
        private final String code;

        public FetchException(String code) {
            super(code);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    /**
     * Downloads the page of an allow-listed source.
     *
     * @throws FetchException SOURCE_NOT_ALLOWED, SOURCE_UNREACHABLE, SOURCE_TOO_LARGE or UNSUPPORTED_CONTENT_TYPE
     */
    String fetch(URI uri);
}
