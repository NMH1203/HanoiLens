package vn.edu.usth.hanoilens.data.model;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    @SerializedName("data")
    private T data;

    @SerializedName("pagination")
    private PaginationMetadata pagination;

    public ApiResponse() {}

    public ApiResponse(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public PaginationMetadata getPagination() {
        return pagination;
    }

    public void setPagination(PaginationMetadata pagination) {
        this.pagination = pagination;
    }
}
