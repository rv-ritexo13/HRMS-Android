package com.triotech.hrms.core.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

/**
 * View-binding-aware Fragment base. Subclasses implement {@link #inflateBinding}
 * and get a non-null {@link #binding} between onCreateView and onDestroyView,
 * with the reference cleared automatically to avoid leaking the view hierarchy.
 *
 * @param <VB> the generated ViewBinding type for this fragment's layout.
 */
public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    private VB _binding;

    /** Non-null only between onCreateView and onDestroyView. */
    @NonNull
    protected VB getBinding() {
        if (_binding == null) {
            throw new IllegalStateException(
                    "Binding accessed outside of the view lifecycle (" + getClass().getSimpleName() + ")");
        }
        return _binding;
    }

    protected abstract VB inflateBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = inflateBinding(inflater, container);
        return _binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        onBindingReady(savedInstanceState);
    }

    /** Subclasses set up views, observers and listeners here instead of onViewCreated. */
    protected abstract void onBindingReady(@Nullable Bundle savedInstanceState);

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
