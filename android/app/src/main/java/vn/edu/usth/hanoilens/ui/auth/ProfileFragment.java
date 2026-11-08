package vn.edu.usth.hanoilens.ui.auth;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vn.edu.usth.hanoilens.databinding.FragmentProfileBinding;
import vn.edu.usth.hanoilens.ui.base.BaseFragment;

public class ProfileFragment extends BaseFragment<FragmentProfileBinding> {

    @Override
    protected FragmentProfileBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentProfileBinding.inflate(inflater, container, false);
    }
}
