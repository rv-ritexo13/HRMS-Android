package com.triotech.hrms.ui.salary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.triotech.hrms.core.base.BaseViewModel;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Payslip;
import com.triotech.hrms.data.repository.SalaryRepository;
import java.util.List;

/**
 * Backs {@link SalaryFragment}. Loads the full salary history from the local
 * database as one {@code Resource} stream (driving Loading/Empty/Error/Success),
 * and tracks which month is selected so the summary + breakdown at the top of the
 * screen and the highlighted row in the history list stay in sync. The selected
 * payslip is derived from the already-loaded list — no extra query — while the
 * Payslip detail screen re-reads its month from the DB by key.
 */
public class SalaryViewModel extends BaseViewModel {

    private final SalaryRepository repository;

    private final MediatorLiveData<Resource<List<Payslip>>> history = new MediatorLiveData<>();
    private final MutableLiveData<String> selectedMonthKey = new MutableLiveData<>();
    private final MediatorLiveData<Payslip> selectedPayslip = new MediatorLiveData<>();

    @Nullable private LiveData<Resource<List<Payslip>>> currentSource;

    public SalaryViewModel(@NonNull SalaryRepository repository) {
        this.repository = repository;

        selectedPayslip.addSource(history, resource -> recomputeSelection(resource, selectedMonthKey.getValue()));
        selectedPayslip.addSource(selectedMonthKey, key -> recomputeSelection(history.getValue(), key));

        load();
    }

    @NonNull
    public LiveData<Resource<List<Payslip>>> getHistory() {
        return history;
    }

    @NonNull
    public LiveData<Payslip> getSelectedPayslip() {
        return selectedPayslip;
    }

    @NonNull
    public LiveData<String> getSelectedMonthKey() {
        return selectedMonthKey;
    }

    public void selectMonth(@NonNull String monthKey) {
        selectedMonthKey.setValue(monthKey);
    }

    public void retry() {
        load();
    }

    private void load() {
        if (currentSource != null) {
            history.removeSource(currentSource);
        }
        currentSource = repository.observeSalaryHistory();
        history.addSource(currentSource, history::setValue);
    }

    /** Keeps {@link #selectedPayslip} pointing at a valid row: the chosen month, or the newest if none/invalid. */
    private void recomputeSelection(
            @Nullable Resource<List<Payslip>> resource, @Nullable String key) {
        if (resource == null || !resource.isSuccess() || resource.data == null || resource.data.isEmpty()) {
            return;
        }
        List<Payslip> payslips = resource.data;
        Payslip match = null;
        if (key != null) {
            for (Payslip p : payslips) {
                if (p.getMonthKey().equals(key)) {
                    match = p;
                    break;
                }
            }
        }
        if (match == null) {
            match = payslips.get(0); // list is newest-first
            if (!match.getMonthKey().equals(selectedMonthKey.getValue())) {
                selectedMonthKey.setValue(match.getMonthKey());
                return; // the selectedMonthKey change re-triggers this method
            }
        }
        selectedPayslip.setValue(match);
    }
}
