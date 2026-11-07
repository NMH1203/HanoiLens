package vn.edu.usth.hanoilens.shared;

/** Single-resource envelope: {@code {"data": ...}}. {@code data} is always serialised, even when null. */
public record ApiResponse<T>(T data) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
