package vn.edu.usth.hanoilens.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import vn.edu.usth.hanoilens.R;
import vn.edu.usth.hanoilens.data.local.DestinationEntity;
import vn.edu.usth.hanoilens.data.repository.DestinationRepository;
import java.util.List;

public class ExploreViewModel extends BaseViewModel {

    private final DestinationRepository repository;
    private final LiveData<List<DestinationEntity>> destinations;

    public ExploreViewModel(@NonNull Application application) {
        super(application);
        this.repository = new DestinationRepository(application);
        this.destinations = repository.getCachedDestinations();
    }

    public LiveData<List<DestinationEntity>> getDestinations() {
        return destinations;
    }

    public void refreshDestinations() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        repository.refreshDestinations(new DestinationRepository.RefreshCallback() {
            @Override
            public void onSuccess() {
                isLoading.postValue(false);
            }

            @Override
            public void onError() {
                isLoading.postValue(false);
                errorMessage.postValue(getApplication().getString(R.string.explore_load_error));
            }
        });
    }
}
