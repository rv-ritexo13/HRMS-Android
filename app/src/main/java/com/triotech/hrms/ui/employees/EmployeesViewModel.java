package com.triotech.hrms.ui.employees;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Employee;
import com.triotech.hrms.data.repository.EmployeeRepository;
import java.util.List;

/**
 * Backs {@link EmployeesFragment}. Exposes a single {@code Resource<List<Employee>>}
 * stream so the Fragment can drive Loading/Empty/Error/Success UI from one observer,
 * and supports pull-to-refresh by re-subscribing to a fresh repository call.
 */
public class EmployeesViewModel extends BaseViewModel {

    private final EmployeeRepository employeeRepository;
    private final MediatorLiveData<Resource<List<Employee>>> employees = new MediatorLiveData<>();
    @Nullable private LiveData<Resource<List<Employee>>> currentSource;

    public EmployeesViewModel(@NonNull EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
        load();
    }

    @NonNull
    public LiveData<Resource<List<Employee>>> getEmployees() {
        return employees;
    }

    public void load() {
        if (currentSource != null) {
            employees.removeSource(currentSource);
        }
        currentSource = employeeRepository.getEmployees();
        employees.addSource(currentSource, employees::setValue);
    }

    public void retry() {
        load();
    }
}
