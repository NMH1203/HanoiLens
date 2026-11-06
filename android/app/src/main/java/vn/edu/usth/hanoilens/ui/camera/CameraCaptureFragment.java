package vn.edu.usth.hanoilens.ui.camera;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import vn.edu.usth.hanoilens.databinding.FragmentCameraCaptureBinding;
import vn.edu.usth.hanoilens.ui.base.BaseFragment;

public class CameraCaptureFragment extends BaseFragment<FragmentCameraCaptureBinding> {

    @Override
    protected FragmentCameraCaptureBinding inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentCameraCaptureBinding.inflate(inflater, container, false);
    }
}
