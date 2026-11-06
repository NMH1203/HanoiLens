package vn.edu.usth.hanoilens.data.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PATCH;
import vn.edu.usth.hanoilens.data.model.ApiResponse;
import vn.edu.usth.hanoilens.data.model.AuthResponseDto;
import vn.edu.usth.hanoilens.data.model.LoginRequest;
import vn.edu.usth.hanoilens.data.model.RegisterRequest;
import vn.edu.usth.hanoilens.data.model.RefreshTokenRequest;
import com.google.gson.JsonObject;

public interface AuthApi {

    @POST("auth/register")
    Call<ApiResponse<JsonObject>> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<ApiResponse<AuthResponseDto>> login(@Body LoginRequest request);

    @POST("auth/refresh")
    Call<ApiResponse<AuthResponseDto>> refresh(@Body RefreshTokenRequest request);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout();

    @GET("me")
    Call<ApiResponse<JsonObject>> getCurrentProfile();

    @PATCH("me")
    Call<ApiResponse<JsonObject>> updateCurrentProfile(@Body JsonObject patch);
}
