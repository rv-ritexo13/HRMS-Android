package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Employee;
import java.util.List;

/**
 * Employee data source contract. {@link FakeEmployeeRepository} backs this with
 * an in-memory mock dataset for Phase 1; a Phase 2 network/database-backed
 * implementation can be swapped in via {@link com.triotech.hrms.core.di.ServiceLocator}
 * without touching any ViewModel or Fragment.
 */
public interface EmployeeRepository {

    @NonNull
    LiveData<Resource<List<Employee>>> getEmployees();

    /** Dashboard summary counts — team size, present today, on leave, open roles. */
    @NonNull
    LiveData<Resource<DashboardStats>> getDashboardStats();

    /** Simple immutable holder for the dashboard's headline numbers. */
    final class DashboardStats {
        public final int totalEmployees;
        public final int presentToday;
        public final int onLeave;
        public final int openRoles;

        public DashboardStats(int totalEmployees, int presentToday, int onLeave, int openRoles) {
            this.totalEmployees = totalEmployees;
            this.presentToday = presentToday;
            this.onLeave = onLeave;
            this.openRoles = openRoles;
        }
    }
}
