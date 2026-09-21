package com.triotech.hrms.ui.performance;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.chip.Chip;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Goal;
import com.triotech.hrms.data.model.Kra;
import com.triotech.hrms.data.model.PerformanceOverview;
import com.triotech.hrms.data.model.PerformanceReview;
import com.triotech.hrms.data.repository.PerformanceRepository;
import com.triotech.hrms.databinding.FragmentPerformanceBinding;
import com.triotech.hrms.databinding.ItemGoalBinding;
import com.triotech.hrms.databinding.ItemKraBinding;
import com.triotech.hrms.databinding.ItemPerformanceReviewBinding;

/**
 * Performance Management screen (opened from the Profile tab): the employee's
 * appraisal cycles, goals/objectives and Key Result Areas for the current cycle.
 * Everything is mock data via {@link PerformanceRepository}; the three sections
 * are populated into plain containers since the lists are short and static.
 */
public class PerformanceFragment extends BaseFragment<FragmentPerformanceBinding> {

    private PerformanceViewModel viewModel;

    @Override
    protected FragmentPerformanceBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentPerformanceBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        getBinding().toolbar.setNavigationOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());

        PerformanceRepository repository = ServiceLocator.getInstance().getPerformanceRepository();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(() -> new PerformanceViewModel(repository)))
                .get(PerformanceViewModel.class);

        getBinding().errorView.setTitle(R.string.performance_error_title);
        getBinding().errorView.setMessage(R.string.performance_error_message);
        getBinding().errorView.setOnRetryListener(viewModel::retry);
        getBinding().swipeRefresh.setOnRefreshListener(viewModel::retry);

        viewModel.getPerformance().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(@NonNull Resource<PerformanceOverview> resource) {
        boolean success = resource.isSuccess() && resource.data != null;
        getBinding().swipeRefresh.setRefreshing(resource.isLoading() && success);
        getBinding().loadingView.setVisibility(resource.isLoading() && !success ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);
        getBinding().swipeRefresh.setVisibility(success ? View.VISIBLE : View.GONE);

        if (success) {
            bind(resource.data);
        }
    }

    private void bind(@NonNull PerformanceOverview overview) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        getBinding().containerReviews.removeAllViews();
        for (PerformanceReview review : overview.getReviews()) {
            getBinding().containerReviews.addView(buildReviewRow(inflater, getBinding().containerReviews, review));
        }

        getBinding().containerGoals.removeAllViews();
        for (Goal goal : overview.getGoals()) {
            getBinding().containerGoals.addView(buildGoalRow(inflater, getBinding().containerGoals, goal));
        }

        getBinding().containerKras.removeAllViews();
        for (Kra kra : overview.getKras()) {
            getBinding().containerKras.addView(buildKraRow(inflater, getBinding().containerKras, kra));
        }
    }

    // ===================== Reviews =====================

    @NonNull
    private View buildReviewRow(
            @NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull PerformanceReview review) {
        ItemPerformanceReviewBinding row = ItemPerformanceReviewBinding.inflate(inflater, parent, false);
        row.textReviewCycle.setText(review.getCycleName());
        row.textReviewPeriod.setText(review.getPeriodLabel());
        row.textReviewReviewer.setText(getString(R.string.performance_reviewer_format, review.getReviewerName()));
        row.textReviewRating.setText(review.getRatingLabel() != null
                ? getString(R.string.performance_rating_format, review.getRatingLabel())
                : getString(R.string.performance_rating_pending));
        applyStatusChip(row.chipReviewStatus, reviewStatusLabel(review.getStatus()),
                reviewStatusColor(review.getStatus()));
        return row.getRoot();
    }

    @StringRes
    private static int reviewStatusLabel(@NonNull PerformanceReview.Status status) {
        switch (status) {
            case COMPLETED:
                return R.string.performance_status_completed;
            case IN_PROGRESS:
                return R.string.performance_status_in_progress;
            case UPCOMING:
            default:
                return R.string.performance_status_upcoming;
        }
    }

    @ColorRes
    private static int reviewStatusColor(@NonNull PerformanceReview.Status status) {
        switch (status) {
            case COMPLETED:
                return R.color.hrms_status_success;
            case IN_PROGRESS:
                return R.color.hrms_status_warning;
            case UPCOMING:
            default:
                return R.color.hrms_status_info;
        }
    }

    // ===================== Goals =====================

    @NonNull
    private View buildGoalRow(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Goal goal) {
        ItemGoalBinding row = ItemGoalBinding.inflate(inflater, parent, false);
        row.textGoalTitle.setText(goal.getTitle());
        row.textGoalDescription.setText(goal.getDescription());
        row.progressGoal.setProgressCompat(goal.getProgressPercent(), false);
        row.textGoalProgress.setText(getString(R.string.performance_goal_progress_format, goal.getProgressPercent()));
        row.textGoalDue.setText(getString(R.string.performance_goal_due_format, goal.getDueLabel()));
        applyStatusChip(row.chipGoalStatus, goalStatusLabel(goal.getStatus()), goalStatusColor(goal.getStatus()));
        return row.getRoot();
    }

    @StringRes
    private static int goalStatusLabel(@NonNull Goal.Status status) {
        switch (status) {
            case ON_TRACK:
                return R.string.performance_goal_on_track;
            case AT_RISK:
                return R.string.performance_goal_at_risk;
            case COMPLETED:
            default:
                return R.string.performance_goal_completed;
        }
    }

    @ColorRes
    private static int goalStatusColor(@NonNull Goal.Status status) {
        switch (status) {
            case ON_TRACK:
                return R.color.hrms_status_info;
            case AT_RISK:
                return R.color.hrms_status_warning;
            case COMPLETED:
            default:
                return R.color.hrms_status_success;
        }
    }

    // ===================== KRAs =====================

    @NonNull
    private View buildKraRow(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @NonNull Kra kra) {
        ItemKraBinding row = ItemKraBinding.inflate(inflater, parent, false);
        row.textKraTitle.setText(kra.getTitle());
        row.textKraWeightage.setText(getString(R.string.performance_kra_weightage_format, kra.getWeightagePercent()));
        row.textKraTarget.setText(getString(R.string.performance_kra_target_format, kra.getTargetLabel()));
        row.textKraAchievement.setText(
                getString(R.string.performance_kra_achievement_format, kra.getAchievementLabel()));
        row.textKraRating.setText(getString(R.string.performance_rating_format, kra.getRatingLabel()));
        return row.getRoot();
    }

    // ===================== Helpers =====================

    private void applyStatusChip(@NonNull Chip chip, @StringRes int labelRes, @ColorRes int colorRes) {
        int color = ContextCompat.getColor(requireContext(), colorRes);
        chip.setText(labelRes);
        chip.setTextColor(color);
        chip.setChipBackgroundColor(ColorStateList.valueOf(withAlpha(color, 0x22)));
    }

    /** A translucent container tint derived from the status color, matching the attendance chips' look. */
    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
