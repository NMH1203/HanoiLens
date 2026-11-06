package vn.edu.usth.hanoilens.data.api;

import com.google.gson.JsonObject;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import vn.edu.usth.hanoilens.data.model.ApiResponse;

public interface EventApi {

    @GET("events")
    Call<ApiResponse<List<JsonObject>>> getEvents(
            @Query("cursor") String cursor,
            @Query("limit") Integer limit,
            @Query("from") String from,
            @Query("to") String to,
            @Query("status") String status,
            @Query("district") String district,
            @Query("destinationId") String destinationId,
            @Query("search") String search
    );

    @GET("events/nearby")
    Call<ApiResponse<List<JsonObject>>> getNearbyEvents(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("radiusMeters") Integer radiusMeters,
            @Query("from") String from,
            @Query("to") String to,
            @Query("cursor") String cursor,
            @Query("limit") Integer limit
    );

    @GET("events/{eventId}")
    Call<ApiResponse<JsonObject>> getEvent(@Path("eventId") String eventId);
}
