package vn.edu.usth.hanoilens.ui.events;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vn.edu.usth.hanoilens.databinding.FragmentEventFeedBinding;
import vn.edu.usth.hanoilens.ui.base.BaseFragment;

public class EventFeedFragment extends BaseFragment<FragmentEventFeedBinding> {

    @Override
    protected FragmentEventFeedBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentEventFeedBinding.inflate(inflater, container, false);
    }
}
