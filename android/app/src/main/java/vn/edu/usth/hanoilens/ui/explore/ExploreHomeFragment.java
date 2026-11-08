package vn.edu.usth.hanoilens.ui.explore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import vn.edu.usth.hanoilens.R;
import vn.edu.usth.hanoilens.databinding.FragmentExploreHomeBinding;
import vn.edu.usth.hanoilens.ui.base.BaseFragment;
import vn.edu.usth.hanoilens.viewmodel.ExploreViewModel;

public class ExploreHomeFragment extends BaseFragment<FragmentExploreHomeBinding> {

    private ExploreViewModel viewModel;

    @Override
    protected FragmentExploreHomeBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentExploreHomeBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ExploreViewModel.class);

        setupObservers();

        // Trigger initial refresh from network to populate local Room cache
        viewModel.refreshDestinations();
    }

    private void setupObservers() {
        // Observe offline cache from Room
        viewModel.getDestinations().observe(getViewLifecycleOwner(), destinations -> {
            if (destinations != null && !destinations.isEmpty()) {
                getBinding().tvExploreTitle.setText(
                        getString(R.string.nav_explore) + " (" + destinations.size() + " địa điểm)"
                );
            }
        });

        // Observe network loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            getBinding().progressExploreLoading.setVisibility(
                    Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        // Observe error state
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            getBinding().tvExploreError.setText(error);
            getBinding().tvExploreError.setVisibility(
                    error == null || error.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }
}
