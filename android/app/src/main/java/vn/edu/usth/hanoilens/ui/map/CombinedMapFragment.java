package vn.edu.usth.hanoilens.ui.map;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vn.edu.usth.hanoilens.databinding.FragmentCombinedMapBinding;
import vn.edu.usth.hanoilens.ui.base.BaseFragment;

public class CombinedMapFragment extends BaseFragment<FragmentCombinedMapBinding> {

    @Override
    protected FragmentCombinedMapBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentCombinedMapBinding.inflate(inflater, container, false);
    }
}
