package com.triotech.hrms.data.repository;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Employee;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * In-memory mock implementation of {@link EmployeeRepository}.
 *
 * <p>Simulates real async latency with a short {@link Handler} delay so screens
 * genuinely exercise their Loading state instead of always resolving instantly —
 * useful for eyeballing the design system's loading/empty/error components with
 * realistic timing before any real backend exists.</p>
 */
public class FakeEmployeeRepository implements EmployeeRepository {

    private static final long SIMULATED_LATENCY_MS = 600L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @NonNull
    @Override
    public LiveData<Resource<List<Employee>>> getEmployees() {
        MutableLiveData<Resource<List<Employee>>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading());
        mainHandler.postDelayed(() -> {
            List<Employee> employees = mockEmployees();
            liveData.setValue(employees.isEmpty() ? Resource.empty() : Resource.success(employees));
        }, SIMULATED_LATENCY_MS);
        return liveData;
    }

    @NonNull
    @Override
    public LiveData<Resource<DashboardStats>> getDashboardStats() {
        MutableLiveData<Resource<DashboardStats>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading());
        mainHandler.postDelayed(() -> {
            List<Employee> employees = mockEmployees();
            int onLeave = 0;
            for (Employee e : employees) {
                if (e.getStatus() == Employee.EmploymentStatus.ON_LEAVE) {
                    onLeave++;
                }
            }
            DashboardStats stats = new DashboardStats(
                    employees.size(),
                    employees.size() - onLeave,
                    onLeave,
                    3);
            liveData.setValue(Resource.success(stats));
        }, SIMULATED_LATENCY_MS);
        return liveData;
    }

    @NonNull
    private static List<Employee> mockEmployees() {
        return new ArrayList<>(Arrays.asList(
                new Employee("EMP-1001", "Aditi Sharma", "Senior Product Designer", "Design",
                        Employee.EmploymentStatus.ACTIVE),
                new Employee("EMP-1002", "Rohan Mehta", "Backend Engineer", "Engineering",
                        Employee.EmploymentStatus.ACTIVE),
                new Employee("EMP-1003", "Kavya Iyer", "HR Business Partner", "People Ops",
                        Employee.EmploymentStatus.ON_LEAVE),
                new Employee("EMP-1004", "Arjun Nair", "QA Engineer", "Engineering",
                        Employee.EmploymentStatus.ACTIVE),
                new Employee("EMP-1005", "Priya Das", "Finance Analyst", "Finance",
                        Employee.EmploymentStatus.NOTICE_PERIOD),
                new Employee("EMP-1006", "Vikram Rao", "Engineering Manager", "Engineering",
                        Employee.EmploymentStatus.ACTIVE),
                new Employee("EMP-1007", "Neha Gupta", "Talent Acquisition Lead", "People Ops",
                        Employee.EmploymentStatus.ACTIVE),
                new Employee("EMP-1008", "Sameer Khan", "DevOps Engineer", "Engineering",
                        Employee.EmploymentStatus.ON_LEAVE),
                new Employee("EMP-1009", "Ishita Verma", "Marketing Manager", "Marketing",
                        Employee.EmploymentStatus.ACTIVE),
                new Employee("EMP-1010", "Karan Malhotra", "Sales Executive", "Sales",
                        Employee.EmploymentStatus.ACTIVE)
        ));
    }
}
