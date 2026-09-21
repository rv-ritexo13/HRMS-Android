package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Payslip;
import java.util.List;

/**
 * Data contract for the Salary &amp; Payslip module. Backed for now by
 * {@link DbSalaryRepository} reading a seeded local SQLite table; a real payroll
 * API can replace it behind {@link com.triotech.hrms.core.di.ServiceLocator}
 * without any UI change, since every method already returns
 * {@code LiveData<Resource<T>>}.
 */
public interface SalaryRepository {

    /** All months of salary history for the signed-in employee, most recent first. */
    @NonNull
    LiveData<Resource<List<Payslip>>> observeSalaryHistory();

    /** A single month's payslip by its "yyyy-MM" key, or an error if not found. */
    @NonNull
    LiveData<Resource<Payslip>> observePayslip(@NonNull String monthKey);
}
