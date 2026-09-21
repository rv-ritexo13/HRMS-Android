package com.triotech.hrms.core.di;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.triotech.hrms.data.repository.AttendanceRepository;
import com.triotech.hrms.data.repository.AuthRepository;
import com.triotech.hrms.data.repository.DashboardContentRepository;
import com.triotech.hrms.data.repository.DbProfileRepository;
import com.triotech.hrms.data.repository.DbSalaryRepository;
import com.triotech.hrms.data.repository.DocumentRepository;
import com.triotech.hrms.data.repository.EmployeeRepository;
import com.triotech.hrms.data.repository.FakeAttendanceRepository;
import com.triotech.hrms.data.repository.FakeDashboardContentRepository;
import com.triotech.hrms.data.repository.DbAuthRepository;
import com.triotech.hrms.data.repository.DbLeaveRepository;
import com.triotech.hrms.data.repository.FakeEmployeeRepository;
import com.triotech.hrms.data.repository.LeaveRepository;
import com.triotech.hrms.data.repository.MockDocumentRepository;
import com.triotech.hrms.data.repository.ProfileRepository;
import com.triotech.hrms.data.repository.SalaryRepository;

/**
 * Minimal hand-rolled service locator, kept from Phase 1.
 *
 * <p>The app is deliberately kept free of a DI framework (Hilt/Dagger) while it only
 * wires a handful of mock repositories — pulling in annotation processors this early
 * adds build-time risk for no real benefit yet. Repositories are exposed as
 * interfaces so swapping this class for Hilt modules in a later phase is a
 * mechanical, low-risk change: {@code @Inject} constructors replace the {@code new}
 * calls below, nothing in the ViewModels needs to change.</p>
 */
public final class ServiceLocator {

    private static volatile ServiceLocator instance;
    @Nullable private static volatile Context appContext;

    private final EmployeeRepository employeeRepository;
    private final AuthRepository authRepository;
    private final AttendanceRepository attendanceRepository;
    private final DashboardContentRepository dashboardContentRepository;
    private final SalaryRepository salaryRepository;
    private final ProfileRepository profileRepository;
    private final DocumentRepository documentRepository;
    private final LeaveRepository leaveRepository;

    private ServiceLocator() {
        this.employeeRepository = new FakeEmployeeRepository();
        this.authRepository = new DbAuthRepository(requireContext());
        this.attendanceRepository = new FakeAttendanceRepository(appContext);
        this.dashboardContentRepository = new FakeDashboardContentRepository();
        this.salaryRepository = new DbSalaryRepository(requireContext());
        this.profileRepository = new DbProfileRepository(requireContext());
        this.documentRepository = new MockDocumentRepository();
        this.leaveRepository = new DbLeaveRepository(requireContext());
    }

    /**
     * Called once from {@link com.triotech.hrms.HrmsApplication#onCreate()}, before any
     * Activity is created, so the first {@link #getInstance()} call already has a Context
     * available for repositories that persist to SharedPreferences.
     */
    public static void init(@NonNull Context context) {
        appContext = context.getApplicationContext();
    }

    @NonNull
    public static ServiceLocator getInstance() {
        if (instance == null) {
            synchronized (ServiceLocator.class) {
                if (instance == null) {
                    instance = new ServiceLocator();
                }
            }
        }
        return instance;
    }

    @NonNull
    public EmployeeRepository getEmployeeRepository() {
        return employeeRepository;
    }

    @NonNull
    public AuthRepository getAuthRepository() {
        return authRepository;
    }

    @NonNull
    public AttendanceRepository getAttendanceRepository() {
        return attendanceRepository;
    }

    @NonNull
    public DashboardContentRepository getDashboardContentRepository() {
        return dashboardContentRepository;
    }

    @NonNull
    public SalaryRepository getSalaryRepository() {
        return salaryRepository;
    }

    @NonNull
    public ProfileRepository getProfileRepository() {
        return profileRepository;
    }

    @NonNull
    public DocumentRepository getDocumentRepository() {
        return documentRepository;
    }

    @NonNull
    public LeaveRepository getLeaveRepository() {
        return leaveRepository;
    }

    @NonNull
    private static Context requireContext() {
        Context context = appContext;
        if (context == null) {
            throw new IllegalStateException(
                    "ServiceLocator.init(context) must be called before getInstance()");
        }
        return context;
    }

    @VisibleForTesting
    public static void setInstanceForTesting(@NonNull ServiceLocator testInstance) {
        instance = testInstance;
    }
}
