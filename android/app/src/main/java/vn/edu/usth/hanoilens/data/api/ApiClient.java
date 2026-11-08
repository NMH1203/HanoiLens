package vn.edu.usth.hanoilens.data.api;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import vn.edu.usth.hanoilens.BuildConfig;
import vn.edu.usth.hanoilens.data.local.TokenManager;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static volatile Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    Context appContext = context.getApplicationContext();
                    Interceptor authInterceptor = chain -> {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder()
                                .header("Accept", "application/json")
                                .header("Content-Type", "application/json");

                        String path = original.url().encodedPath();
                        boolean publicAuthRequest = path.endsWith("/auth/login")
                                || path.endsWith("/auth/register")
                                || path.endsWith("/auth/refresh");
                        if (!publicAuthRequest) {
                            String token = TokenManager.getInstance(appContext).getAccessToken();
                            if (token != null && !token.isEmpty()) {
                                builder.header("Authorization", "Bearer " + token);
                            }
                        }
                        return chain.proceed(builder.build());
                    };

                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    // Never log bodies: arrival and nearby requests may carry exact coordinates.
                    logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.HEADERS : HttpLoggingInterceptor.Level.NONE);
                    // Keep credentials and precise-location query parameters out of debug logs.
                    logging.redactHeader("Authorization");
                    logging.redactHeader("Proxy-Authorization");
                    logging.redactHeader("Cookie");
                    logging.redactHeader("Set-Cookie");

                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(authInterceptor)
                            .addInterceptor(logging)
                            .authenticator(new TokenAuthenticator(appContext, BuildConfig.API_BASE_URL))
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .build();

                    Gson gson = new GsonBuilder().create();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BuildConfig.API_BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();
                }
            }
        }
        return retrofit;
    }
}
