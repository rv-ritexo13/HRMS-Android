package com.triotech.hrms.ui.salary;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.CurrencyUtils;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.core.util.PayslipPdfGenerator;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.data.model.Payslip;
import com.triotech.hrms.data.repository.SalaryRepository;
import com.triotech.hrms.databinding.FragmentPayslipBinding;
import com.triotech.hrms.databinding.ItemSalaryComponentBinding;
import java.io.File;
import java.util.Locale;

/**
 * Professional payslip detail for a single month. Reads the month from the local
 * database (by "yyyy-MM" key passed as a nav argument) and lets the employee open,
 * download or share a real PDF generated with {@link PayslipPdfGenerator}. Opened
 * as a full-screen destination (bottom nav hidden) with a back arrow in the toolbar.
 */
public class PayslipFragment extends BaseFragment<FragmentPayslipBinding> {

    private PayslipViewModel viewModel;
    @Nullable private Payslip payslip;

    @Override
    protected FragmentPayslipBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentPayslipBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        String monthKey = getArguments() != null
                ? getArguments().getString(SalaryFragment.ARG_MONTH_KEY) : null;
        if (monthKey == null) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        getBinding().toolbar.setNavigationOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());

        SalaryRepository repository = ServiceLocator.getInstance().getSalaryRepository();
        viewModel = new ViewModelProvider(this,
                new ViewModelFactory(() -> new PayslipViewModel(repository, monthKey)))
                .get(PayslipViewModel.class);

        getBinding().errorView.setTitle(R.string.salary_error_title);
        getBinding().errorView.setMessage(R.string.salary_error_message);

        setActionsEnabled(false);
        getBinding().buttonViewPdf.setOnClickListener(v -> onViewPdf());
        getBinding().buttonDownload.setOnClickListener(v -> onDownload());
        getBinding().buttonShare.setOnClickListener(v -> onShare());

        viewModel.getPayslip().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(@NonNull Resource<Payslip> resource) {
        getBinding().loadingView.setVisibility(resource.isLoading() ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);
        getBinding().contentContainer.setVisibility(resource.isSuccess() ? View.VISIBLE : View.GONE);

        if (resource.isSuccess() && resource.data != null) {
            payslip = resource.data;
            bind(resource.data);
            setActionsEnabled(true);
        }
    }

    private void bind(@NonNull Payslip p) {
        getBinding().textPayPeriod.setText(DateUtils.formatMonthYear(p.getYear(), p.getMonth()));
        getBinding().textEmployeeName.setText(p.getEmployeeName());
        getBinding().textEmployeeId.setText(p.getEmployeeId());
        getBinding().textDepartment.setText(p.getDepartment());
        getBinding().textDesignation.setText(p.getDesignation());

        String creditedPrefix = getString(p.isCredited() ? R.string.salary_credited_on : R.string.salary_pending);
        getBinding().textPayDate.setText(creditedPrefix + " · " + DateUtils.formatLongDate(p.getCreditDateMillis()));
        getBinding().textNetPayable.setText(CurrencyUtils.formatRupees(p.getNetSalary()));

        row(getBinding().pRowBasic, R.string.salary_component_basic, p.getBasic());
        row(getBinding().pRowHra, R.string.salary_component_hra, p.getHra());
        row(getBinding().pRowSpecial, R.string.salary_component_special, p.getSpecialAllowance());
        row(getBinding().pRowOtherAllowances, R.string.salary_component_other_allowances, p.getOtherAllowances());
        row(getBinding().pRowGross, R.string.salary_gross_total, p.getGrossSalary());

        row(getBinding().pRowPf, R.string.salary_component_pf, p.getProvidentFund());
        row(getBinding().pRowPtax, R.string.salary_component_ptax, p.getProfessionalTax());
        row(getBinding().pRowOtherDeductions, R.string.salary_component_other_deductions, p.getOtherDeductions());
        row(getBinding().pRowTotalDeductions, R.string.salary_deductions_total, p.getTotalDeductions());
    }

    private void row(@NonNull ItemSalaryComponentBinding row, int labelRes, long amount) {
        row.textComponentLabel.setText(labelRes);
        row.textComponentAmount.setText(CurrencyUtils.formatRupees(amount));
    }

    private void setActionsEnabled(boolean enabled) {
        getBinding().buttonViewPdf.setEnabled(enabled);
        getBinding().buttonDownload.setEnabled(enabled);
        getBinding().buttonShare.setEnabled(enabled);
    }

    // ===================== PDF actions =====================

    private void onViewPdf() {
        File file = generateToCache();
        if (file == null) {
            return;
        }
        Uri uri = uriFor(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast(getString(R.string.payslip_error_no_viewer));
        }
    }

    private void onDownload() {
        if (payslip == null) {
            return;
        }
        File dir = new File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Payslips");
        File target = new File(dir, fileName(payslip));
        try {
            PayslipPdfGenerator.writePdf(payslip, target);
            toast(getString(R.string.payslip_downloaded_format, target.getAbsolutePath()));
        } catch (Exception e) {
            toast(getString(R.string.payslip_error_generating));
        }
    }

    private void onShare() {
        if (payslip == null) {
            return;
        }
        File file = generateToCache();
        if (file == null) {
            return;
        }
        Uri uri = uriFor(file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.payslip_share_subject_format,
                DateUtils.formatMonthYear(payslip.getYear(), payslip.getMonth())));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.payslip_share_chooser)));
    }

    @Nullable
    private File generateToCache() {
        if (payslip == null) {
            return null;
        }
        File dir = new File(requireContext().getCacheDir(), "payslips");
        File target = new File(dir, fileName(payslip));
        try {
            return PayslipPdfGenerator.writePdf(payslip, target);
        } catch (Exception e) {
            toast(getString(R.string.payslip_error_generating));
            return null;
        }
    }

    @NonNull
    private Uri uriFor(@NonNull File file) {
        return FileProvider.getUriForFile(
                requireContext(), requireContext().getPackageName() + ".fileprovider", file);
    }

    @NonNull
    private static String fileName(@NonNull Payslip p) {
        return String.format(Locale.US, "Payslip_%s_%s.pdf", p.getEmployeeId(), p.getMonthKey());
    }

    private void toast(@NonNull String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }
}
