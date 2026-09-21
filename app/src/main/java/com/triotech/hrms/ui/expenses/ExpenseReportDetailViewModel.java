package com.triotech.hrms.ui.expenses;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Expense;
import com.triotech.hrms.data.model.ExpenseReport;
import com.triotech.hrms.data.repository.ExpenseRepository;
import java.util.List;

/**
 * Backs {@link ExpenseReportDetailFragment}. Loads all expenses and keeps only
 * the single month's report identified by {@code monthKey}.
 */
public class ExpenseReportDetailViewModel extends BaseViewModel {

    private final ExpenseRepository repository;
    private final String monthKey;
    private final MediatorLiveData<Resource<ExpenseReport>> report = new MediatorLiveData<>();
    @Nullable private LiveData<Resource<List<Expense>>> source;

    public ExpenseReportDetailViewModel(@NonNull ExpenseRepository repository, @NonNull String monthKey) {
        this.repository = repository;
        this.monthKey = monthKey;
        refresh();
    }

    @NonNull
    public LiveData<Resource<ExpenseReport>> getReport() {
        return report;
    }

    public void refresh() {
        if (source != null) {
            report.removeSource(source);
        }
        source = repository.observeExpenses();
        report.addSource(source, resource -> report.setValue(toReport(resource)));
    }

    @NonNull
    private Resource<ExpenseReport> toReport(@NonNull Resource<List<Expense>> resource) {
        if (resource.isLoading()) {
            return Resource.loading();
        }
        if (resource.isError()) {
            return Resource.error(resource.message != null ? resource.message : "Couldn't load this report.");
        }
        if (resource.data == null) {
            return Resource.error("Report not found.");
        }
        for (ExpenseReport candidate : ExpenseReportsViewModel.groupByMonth(resource.data)) {
            if (candidate.getMonthKey().equals(monthKey)) {
                return Resource.success(candidate);
            }
        }
        return Resource.error("Report not found.");
    }
}
