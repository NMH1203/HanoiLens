package vn.edu.usth.hanoilens.data.model;

import com.google.gson.annotations.SerializedName;

public class PaginationMetadata {
    @SerializedName("nextCursor")
    private String nextCursor;

    @SerializedName("hasMore")
    private boolean hasMore;

    public String getNextCursor() {
        return nextCursor;
    }

    public boolean hasMore() {
        return hasMore;
    }
}
