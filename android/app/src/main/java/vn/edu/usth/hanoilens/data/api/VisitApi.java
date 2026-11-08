package vn.edu.usth.hanoilens.data.api;

import com.google.gson.JsonObject;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import vn.edu.usth.hanoilens.data.model.ApiResponse;

public interface VisitApi {

    @POST("visits")
    Call<ApiResponse<JsonObject>> createVisit(
            @Body JsonObject request,
            @Header("Idempotency-Key") String idempotencyKey
    );

    @GET("visits/active")
    Call<ApiResponse<JsonObject>> getActiveVisit();

    @GET("visits")
    Call<ApiResponse<List<JsonObject>>> getVisits(
            @Query("cursor") String cursor,
            @Query("limit") Integer limit,
            @Query("status") String status,
            @Query("destinationId") String destinationId,
            @Query("startedAfter") String startedAfter,
            @Query("startedBefore") String startedBefore
    );

    @GET("visits/{visitId}")
    Call<ApiResponse<JsonObject>> getVisit(@Path("visitId") String visitId);

    @POST("visits/{visitId}/pause")
    Call<ApiResponse<JsonObject>> pauseVisit(
            @Path("visitId") String visitId,
            @Header("Idempotency-Key") String idempotencyKey
    );

    @POST("visits/{visitId}/resume")
    Call<ApiResponse<JsonObject>> resumeVisit(
            @Path("visitId") String visitId,
            @Header("Idempotency-Key") String idempotencyKey
    );

    @POST("visits/{visitId}/end")
    Call<ApiResponse<JsonObject>> endVisit(
            @Path("visitId") String visitId,
            @Body JsonObject request,
            @Header("Idempotency-Key") String idempotencyKey
    );

    @POST("visits/{visitId}/cancel")
    Call<ApiResponse<JsonObject>> cancelVisit(
            @Path("visitId") String visitId,
            @Header("Idempotency-Key") String idempotencyKey
    );

    @PUT("visits/{visitId}/landmarks/{landmarkId}")
    Call<ApiResponse<JsonObject>> markLandmarkVisited(
            @Path("visitId") String visitId,
            @Path("landmarkId") String landmarkId,
            @Body JsonObject request
    );
}
