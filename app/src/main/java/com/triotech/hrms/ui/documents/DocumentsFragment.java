package com.triotech.hrms.ui.documents;

import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.di.ViewModelFactory;
import com.triotech.hrms.core.util.FileShareUtils;
import com.triotech.hrms.core.util.Resource;
import com.triotech.hrms.core.util.SampleDocumentGenerator;
import com.triotech.hrms.data.model.Document;
import com.triotech.hrms.data.repository.DocumentRepository;
import com.triotech.hrms.databinding.FragmentDocumentsBinding;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Documents screen: the employee's document catalog grouped by category with
 * per-row View / Download / Share actions. Files are generated on demand by
 * {@link SampleDocumentGenerator} on a background thread, then opened via the
 * in-app viewer (View) or shared/saved through {@link FileShareUtils}.
 */
public class DocumentsFragment extends BaseFragment<FragmentDocumentsBinding>
        implements DocumentsAdapter.Listener {

    static final String ARG_DOCUMENT_ID = "documentId";

    private DocumentsViewModel viewModel;
    private final DocumentsAdapter adapter = new DocumentsAdapter();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected FragmentDocumentsBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentDocumentsBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        getBinding().toolbar.setNavigationOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());

        getBinding().recyclerDocuments.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().recyclerDocuments.setAdapter(adapter);
        adapter.setListener(this);

        DocumentRepository repository = ServiceLocator.getInstance().getDocumentRepository();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(() -> new DocumentsViewModel(repository)))
                .get(DocumentsViewModel.class);

        getBinding().emptyStateView.setIcon(R.drawable.ic_folder);
        getBinding().emptyStateView.setTitle(R.string.documents_empty_title);
        getBinding().emptyStateView.setMessage(R.string.documents_empty_message);
        getBinding().errorView.setTitle(R.string.documents_error_title);
        getBinding().errorView.setMessage(R.string.documents_error_message);
        getBinding().errorView.setOnRetryListener(viewModel::retry);
        getBinding().swipeRefresh.setOnRefreshListener(viewModel::retry);

        viewModel.getDocuments().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(@NonNull Resource<List<Document>> resource) {
        boolean hasData = adapter.getItemCount() > 0;
        getBinding().swipeRefresh.setRefreshing(resource.isLoading() && hasData);
        getBinding().loadingView.setVisibility(resource.isLoading() && !hasData ? View.VISIBLE : View.GONE);
        getBinding().errorView.setVisibility(resource.isError() ? View.VISIBLE : View.GONE);
        getBinding().emptyStateView.setVisibility(resource.isEmpty() ? View.VISIBLE : View.GONE);
        getBinding().swipeRefresh.setVisibility(resource.isSuccess() ? View.VISIBLE : View.GONE);

        if (resource.isSuccess() && resource.data != null) {
            adapter.submit(buildSectionedList(resource.data));
        }
    }

    /** Flattens the catalog into header + document rows, grouped by category in enum order. */
    @NonNull
    private List<Object> buildSectionedList(@NonNull List<Document> documents) {
        List<Object> rows = new ArrayList<>();
        for (Document.Category category : Document.Category.values()) {
            List<Document> inCategory = new ArrayList<>();
            for (Document d : documents) {
                if (d.getCategory() == category) {
                    inCategory.add(d);
                }
            }
            if (!inCategory.isEmpty()) {
                rows.add(getString(categoryLabel(category)));
                rows.addAll(inCategory);
            }
        }
        return rows;
    }

    private static int categoryLabel(@NonNull Document.Category category) {
        switch (category) {
            case PAYSLIPS:
                return R.string.documents_cat_payslips;
            case OFFER_LETTER:
                return R.string.documents_cat_offer;
            case APPOINTMENT_LETTER:
                return R.string.documents_cat_appointment;
            case EXPERIENCE_LETTER:
                return R.string.documents_cat_experience;
            case TAX_DOCUMENTS:
                return R.string.documents_cat_tax;
            case COMPANY_POLICIES:
                return R.string.documents_cat_policies;
            case OTHER:
            default:
                return R.string.documents_cat_other;
        }
    }

    // ===================== Row actions =====================

    @Override
    public void onView(@NonNull Document document) {
        Bundle args = new Bundle();
        args.putString(ARG_DOCUMENT_ID, document.getId());
        NavHostFragment.findNavController(this).navigate(R.id.action_documents_to_viewer, args);
    }

    @Override
    public void onDownload(@NonNull Document document) {
        File dir = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Documents");
        generate(document, new File(dir, document.getFileName()), file ->
                toast(getString(R.string.documents_downloaded_format, file.getAbsolutePath())));
    }

    @Override
    public void onShare(@NonNull Document document) {
        File dir = new File(requireContext().getCacheDir(), "documents");
        generate(document, new File(dir, document.getFileName()), file -> {
            try {
                FileShareUtils.share(requireContext(), file, document.getFileType().getMimeType(),
                        document.getName(), getString(R.string.documents_action_share));
            } catch (ActivityNotFoundException e) {
                toast(getString(R.string.documents_error_no_viewer));
            }
        });
    }

    private interface OnReady {
        void onReady(@NonNull File file);
    }

    /** Generates the sample file off the main thread, then delivers it back on the main thread. */
    private void generate(@NonNull Document document, @NonNull File target, @NonNull OnReady onReady) {
        ioExecutor.execute(() -> {
            File result = null;
            try {
                result = SampleDocumentGenerator.writeFile(document, target);
            } catch (Exception ignored) {
                // handled below
            }
            File finalResult = result;
            mainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                if (finalResult == null) {
                    toast(getString(R.string.documents_error_open));
                } else {
                    onReady.onReady(finalResult);
                }
            });
        });
    }

    private void toast(@NonNull String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }
}
