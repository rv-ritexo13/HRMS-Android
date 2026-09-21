package com.triotech.hrms.ui.salary;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Payslip;
import com.triotech.hrms.data.repository.SalaryRepository;

/**
 * Backs {@link PayslipFragment}: reads a single month's payslip from the local
 * database by its "yyyy-MM" key and exposes it as one {@code Resource} stream so
 * the detail screen can show Loading / Error / Success consistently.
 */
public class PayslipViewModel extends BaseViewModel {

    private final MediatorLiveData<Resource<Payslip>> payslip = new MediatorLiveData<>();

    public PayslipViewModel(@NonNull SalaryRepository repository, @NonNull String monthKey) {
        payslip.addSource(repository.observePayslip(monthKey), payslip::setValue);
    }

    @NonNull
    public LiveData<Resource<Payslip>> getPayslip() {
        return payslip;
    }
}
