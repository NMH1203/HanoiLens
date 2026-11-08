package vn.edu.usth.hanoilens.data.api;

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import vn.edu.usth.hanoilens.data.local.TokenManager;
import vn.edu.usth.hanoilens.data.model.ApiResponse;
import vn.edu.usth.hanoilens.data.model.AuthResponseDto;
import vn.edu.usth.hanoilens.data.model.RefreshTokenRequest;

/** Refreshes an expired access token once, using a client without auth/logging interceptors. */
public final class TokenAuthenticator implements Authenticator {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Type REFRESH_RESPONSE_TYPE = TypeToken.getParameterized(
            ApiResponse.class, AuthResponseDto.class).getType();

    private final String baseUrl;
    private final TokenManager tokenManager;
    private final OkHttpClient refreshClient;
    private final Gson gson = new GsonBuilder().create();

    public TokenAuthenticator(Context context, String baseUrl) {
        this.baseUrl = baseUrl;
        this.tokenManager = TokenManager.getInstance(context.getApplicationContext());
        this.refreshClient = new OkHttpClient.Builder().build();
    }

    @Nullable
    @Override
    public synchronized Request authenticate(Route route, Response response) throws IOException {
        if (responseCount(response) >= 2) return null;

        String path = response.request().url().encodedPath();
        if (path.endsWith("/auth/login") || path.endsWith("/auth/register")
                || path.endsWith("/auth/refresh")) return null;

        String currentAccessToken = tokenManager.getAccessToken();
        String failedAuthorization = response.request().header("Authorization");
        if (currentAccessToken != null
                && !currentAccessToken.isEmpty()
                && !("Bearer " + currentAccessToken).equals(failedAuthorization)) {
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + currentAccessToken)
                    .build();
        }

        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) return null;

        RequestBody requestBody = RequestBody.create(JSON, gson.toJson(new RefreshTokenRequest(refreshToken)));
        Request refreshRequest = new Request.Builder()
                .url(baseUrl + "auth/refresh")
                .header("Accept", "application/json")
                .post(requestBody)
                .build();

        try (Response refreshResponse = refreshClient.newCall(refreshRequest).execute()) {
            if (refreshResponse.code() == 401) {
                tokenManager.clearTokens();
                return null;
            }
            if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) return null;
            ApiResponse<AuthResponseDto> envelope = gson.fromJson(
                    refreshResponse.body().charStream(), REFRESH_RESPONSE_TYPE);
            AuthResponseDto tokens = envelope == null ? null : envelope.getData();
            if (tokens == null || isBlank(tokens.getAccessToken()) || isBlank(tokens.getRefreshToken())) {
                return null;
            }

            tokenManager.saveTokens(tokens.getAccessToken(), tokens.getRefreshToken());
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + tokens.getAccessToken())
                    .build();
        }
    }

    private int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) count++;
        return count;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
