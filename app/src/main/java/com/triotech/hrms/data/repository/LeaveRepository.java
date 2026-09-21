package com.triotech.hrms.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.LeaveBalance;
import com.triotech.hrms.data.model.LeaveRequest;
import java.util.List;

/**
 * Data contract for the Leave module. Backed by {@link DbLeaveRepository} reading
 * and writing the seeded {@code leave_balances} / {@code leave_requests} tables, so
 * applied and cancelled requests persist. A real leave API can replace it behind
 * {@link com.triotech.hrms.core.di.ServiceLocator} with no UI change.
 */
public interface LeaveRepository {

    /** Leave balances for every leave type. */
    @NonNull
    LiveData<Resource<List<LeaveBalance>>> observeBalances();

    /** All leave requests, most recent (by applied date) first. */
    @NonNull
    LiveData<Resource<List<LeaveRequest>>> observeRequests();

    /** A single request by id, or an error if not found. */
    @NonNull
    LiveData<Resource<LeaveRequest>> observeRequest(@NonNull String id);

    /** Inserts a new request (status PENDING). Emits loading, then success with the saved request. */
    @NonNull
    LiveData<Resource<LeaveRequest>> applyLeave(@NonNull LeaveRequest draft);

    /** Marks a pending request as CANCELLED. Emits loading, then success or error. */
    @NonNull
    LiveData<Resource<Boolean>> cancelRequest(@NonNull String id);
}
