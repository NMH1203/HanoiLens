package vn.edu.usth.hanoilens.data.api;

import com.google.gson.JsonObject;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import vn.edu.usth.hanoilens.data.model.ApiResponse;

public interface MediaApi {

    @POST("media/uploads")
    Call<ApiResponse<JsonObject>> createUpload(
            @Body JsonObject request,
            @Header("Idempotency-Key") String idempotencyKey
    );

    @POST("media/uploads/{uploadId}/complete")
    Call<ApiResponse<JsonObject>> completeUpload(
            @Path("uploadId") String uploadId,
            @Body JsonObject request,
            @Header("Idempotency-Key") String idempotencyKey
    );

    @GET("photos/{photoId}")
    Call<ApiResponse<JsonObject>> getPhoto(@Path("photoId") String photoId);

    @GET("visits/{visitId}/photos")
    Call<ApiResponse<List<JsonObject>>> getVisitPhotos(
            @Path("visitId") String visitId,
            @Query("cursor") String cursor,
            @Query("limit") Integer limit
    );

    @GET("albums")
    Call<ApiResponse<List<JsonObject>>> getAlbums(
            @Query("cursor") String cursor,
            @Query("limit") Integer limit,
            @Query("destinationId") String destinationId
    );

    @GET("albums/{albumId}")
    Call<ApiResponse<JsonObject>> getAlbum(@Path("albumId") String albumId);

    @GET("recaps")
    Call<ApiResponse<List<JsonObject>>> getRecaps(
            @Query("cursor") String cursor,
            @Query("limit") Integer limit,
            @Query("type") String type,
            @Query("status") String status
    );

    @GET("recaps/{recapId}")
    Call<ApiResponse<JsonObject>> getRecap(@Path("recapId") String recapId);
}
