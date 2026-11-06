package vn.edu.usth.hanoilens.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.edu.usth.hanoilens.data.api.ApiClient;
import vn.edu.usth.hanoilens.data.api.DestinationApi;
import vn.edu.usth.hanoilens.data.local.AppDatabase;
import vn.edu.usth.hanoilens.data.local.DestinationDao;
import vn.edu.usth.hanoilens.data.local.DestinationEntity;
import vn.edu.usth.hanoilens.data.model.ApiResponse;
import vn.edu.usth.hanoilens.data.model.DestinationDto;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository bridging Room cache and Retrofit API for heritage destinations.
 * Follows Offline-first architecture.
 */
public class DestinationRepository {

    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private final DestinationDao destinationDao;
    private final DestinationApi destinationApi;

    public interface RefreshCallback {
        void onSuccess();
        void onError();
    }

    public DestinationRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        this.destinationDao = db.destinationDao();
        this.destinationApi = ApiClient.getClient(context).create(DestinationApi.class);
    }

    public LiveData<List<DestinationEntity>> getCachedDestinations() {
        return destinationDao.getAllDestinations();
    }

    public LiveData<DestinationEntity> getDestinationById(String id) {
        return destinationDao.getDestinationById(id);
    }

    public void refreshDestinations(final RefreshCallback callback) {
        destinationApi.getDestinations(null, 20, null, null, null, null, "FEATURED")
                .enqueue(new Callback<ApiResponse<List<DestinationDto>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<DestinationDto>>> call,
                                           Response<ApiResponse<List<DestinationDto>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            List<DestinationDto> dtos = response.body().getData();
                            DB_EXECUTOR.execute(() -> {
                                try {
                                    List<DestinationEntity> entities = new ArrayList<>();
                                    long now = System.currentTimeMillis();
                                    for (DestinationDto dto : dtos) {
                                        entities.add(new DestinationEntity(
                                                dto.getId(),
                                                dto.getSlug(),
                                                dto.getName(),
                                                dto.getArea(),
                                                dto.getDistrict(),
                                                dto.getSummary(),
                                                dto.getCoverImageUrl(),
                                                dto.getBestTime(),
                                                dto.getEstimatedMinutes(),
                                                dto.getLandmarkCount(),
                                                dto.isFeatured(),
                                                now
                                        ));
                                    }
                                    destinationDao.insertAll(entities);
                                    notifySuccess(callback);
                                } catch (RuntimeException exception) {
                                    notifyError(callback);
                                }
                            });
                        } else {
                            notifyError(callback);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<DestinationDto>>> call, Throwable t) {
                        notifyError(callback);
                    }
                });
    }

    private void notifySuccess(RefreshCallback callback) {
        if (callback != null) MAIN_HANDLER.post(callback::onSuccess);
    }

    private void notifyError(RefreshCallback callback) {
        if (callback != null) MAIN_HANDLER.post(callback::onError);
    }
}
