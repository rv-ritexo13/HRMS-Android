package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.PerformanceOverview;

/**
 * Data contract for the employee's performance data (appraisal cycles, goals,
 * KRAs). Backed by {@link MockPerformanceRepository} for now; a real performance
 * service can replace it later without touching the ViewModel or Fragment.
 */
public interface PerformanceRepository {

    /** The full performance overview — reviews, goals and KRAs in one payload. */
    @NonNull
    LiveData<Resource<PerformanceOverview>> observePerformance();
}
