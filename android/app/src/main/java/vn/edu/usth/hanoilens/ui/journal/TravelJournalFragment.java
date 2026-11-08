package vn.edu.usth.hanoilens.ui.journal;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vn.edu.usth.hanoilens.databinding.FragmentTravelJournalBinding;
import vn.edu.usth.hanoilens.ui.base.BaseFragment;

public class TravelJournalFragment extends BaseFragment<FragmentTravelJournalBinding> {

    @Override
    protected FragmentTravelJournalBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentTravelJournalBinding.inflate(inflater, container, false);
    }
}
