package vn.edu.usth.hanoilens.shared;

import java.util.List;

/** Cursor-paginated envelope: {@code {"data": [...], "pagination": {"nextCursor": ..., "hasMore": ...}}}. */
public record PagedResponse<T>(List<T> data, Pagination pagination) {

    public record Pagination(String nextCursor, boolean hasMore) {
    }

    public static <T> PagedResponse<T> of(List<T> data, String nextCursor) {
        return new PagedResponse<>(data, new Pagination(nextCursor, nextCursor != null));
    }
}
