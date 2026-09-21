package com.triotech.hrms.core.di;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import java.util.function.Supplier;

/**
 * Generic {@link ViewModelProvider.Factory} that builds a ViewModel from a
 * {@link Supplier}, so each screen can wire its ViewModel's constructor
 * dependencies (repositories from {@link ServiceLocator}) without every
 * ViewModel needing its own boilerplate Factory class.
 *
 * <pre>
 * viewModel = new ViewModelProvider(this,
 *         new ViewModelFactory(() -> new EmployeesViewModel(
 *                 ServiceLocator.getInstance().getEmployeeRepository())))
 *     .get(EmployeesViewModel.class);
 * </pre>
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    private final Supplier<? extends ViewModel> creator;

    public ViewModelFactory(@NonNull Supplier<? extends ViewModel> creator) {
        this.creator = creator;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        ViewModel viewModel = creator.get();
        if (!modelClass.isInstance(viewModel)) {
            throw new IllegalArgumentException(
                    "Supplier produced " + viewModel.getClass() + " but " + modelClass + " was requested");
        }
        return (T) viewModel;
    }
}
