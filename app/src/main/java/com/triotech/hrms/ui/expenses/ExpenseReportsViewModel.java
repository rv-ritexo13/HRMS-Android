package com.triotech.hrms.ui.expenses;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Expense;
import com.triotech.hrms.data.model.ExpenseReport;
import com.triotech.hrms.data.repository.ExpenseRepository;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Backs {@link ExpenseReportsFragment}. Reads the flat expense list from the
 * repository and rolls it up into one {@link ExpenseReport} per calendar month,
 * most recent month first. Purely derived — no separate reports storage.
 */
public class ExpenseReportsViewModel extends BaseViewModel {

    private final ExpenseRepository repository;
    private final MediatorLiveData<Resource<List<ExpenseReport>>> reports = new MediatorLiveData<>();
    @Nullable private LiveData<Resource<List<Expense>>> source;

    public ExpenseReportsViewModel(@NonNull ExpenseRepository repository) {
        this.repository = repository;
        refresh();
    }

    @NonNull
    public LiveData<Resource<List<ExpenseReport>>> getReports() {
        return reports;
    }

    public void refresh() {
        if (source != null) {
            reports.removeSource(source);
        }
        source = repository.observeExpenses();
        reports.addSource(source, resource -> reports.setValue(toReports(resource)));
    }

    @NonNull
    private static Resource<List<ExpenseReport>> toReports(@NonNull Resource<List<Expense>> resource) {
        if (resource.isLoading()) {
            return Resource.loading();
        }
        if (resource.isError()) {
            return Resource.error(resource.message != null ? resource.message : "Couldn't load reports.");
        }
        if (resource.data == null || resource.data.isEmpty()) {
            return Resource.empty();
        }
        return Resource.success(groupByMonth(resource.data));
    }

    /** Groups expenses (already date-descending from the repo) into month reports, order preserved. */
    @NonNull
    static List<ExpenseReport> groupByMonth(@NonNull List<Expense> expenses) {
        Map<String, List<Expense>> byMonth = new LinkedHashMap<>();
        for (Expense e : expenses) {
            Calendar c = DateUtils.calendarFromMillis(e.getDateMillis());
            String key = String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
            List<Expense> group = byMonth.get(key);
            if (group == null) {
                group = new ArrayList<>();
                byMonth.put(key, group);
            }
            group.add(e);
        }
        List<ExpenseReport> result = new ArrayList<>();
        for (Map.Entry<String, List<Expense>> entry : byMonth.entrySet()) {
            String key = entry.getKey();
            int year = Integer.parseInt(key.substring(0, 4));
            int month = Integer.parseInt(key.substring(5, 7));
            result.add(new ExpenseReport(key, year, month, entry.getValue()));
        }
        return result;
    }
}
