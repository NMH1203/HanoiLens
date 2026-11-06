package vn.edu.usth.hanoilens.data.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import vn.edu.usth.hanoilens.data.model.ApiResponse;
import vn.edu.usth.hanoilens.data.model.DestinationDto;
import com.google.gson.JsonObject;
import java.util.List;

public interface DestinationApi {

    @GET("destinations")
    Call<ApiResponse<List<DestinationDto>>> getDestinations(
            @Query("cursor") String cursor,
            @Query("limit") Integer limit,
            @Query("area") String area,
            @Query("district") String district,
            @Query("isFeatured") Boolean isFeatured,
            @Query("search") String search,
            @Query("sort") String sort
    );

    @GET("destinations/{destinationId}")
    Call<ApiResponse<DestinationDto>> getDestinationDetail(
            @Path("destinationId") String destinationId
    );

    @GET("destinations/{destinationId}/landmarks")
    Call<ApiResponse<List<JsonObject>>> getLandmarks(
            @Path("destinationId") String destinationId,
            @Query("cursor") String cursor,
            @Query("limit") Integer limit
    );

    @POST("destinations/{destinationId}/arrival-checks")
    Call<ApiResponse<JsonObject>> checkArrival(
            @Path("destinationId") String destinationId,
            @Body JsonObject request
    );

    @GET("destinations/nearby")
    Call<ApiResponse<List<DestinationDto>>> getNearbyDestinations(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("radiusMeters") Integer radiusMeters,
            @Query("cursor") String cursor,
            @Query("limit") Integer limit
    );
}
