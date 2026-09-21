package com.triotech.hrms.ui.documents;

import android.content.ActivityNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.triotech.hrms.R;
import com.triotech.hrms.core.base.BaseFragment;
import com.triotech.hrms.core.di.ServiceLocator;
import com.triotech.hrms.core.util.FileShareUtils;
import com.triotech.hrms.core.util.SampleDocumentGenerator;
import com.triotech.hrms.data.model.Document;
import com.triotech.hrms.data.repository.DocumentRepository;
import com.triotech.hrms.databinding.FragmentDocumentViewerBinding;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * In-app document preview. Materialises the document to the cache, then shows a
 * pinch/scroll-zoomable image for JPG/PNG or a scrollable, zoomable list of
 * rasterised pages (via {@link PdfRenderer}) for PDFs. Toolbar actions open the
 * file externally, download a copy, or share it.
 */
public class DocumentViewerFragment extends BaseFragment<FragmentDocumentViewerBinding> {

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final PdfPageAdapter pdfAdapter = new PdfPageAdapter();

    @Nullable private Document document;
    @Nullable private File preparedFile;

    @Override
    protected FragmentDocumentViewerBinding inflateBinding(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentDocumentViewerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onBindingReady(@Nullable Bundle savedInstanceState) {
        String documentId = getArguments() != null
                ? getArguments().getString(DocumentsFragment.ARG_DOCUMENT_ID) : null;
        DocumentRepository repository = ServiceLocator.getInstance().getDocumentRepository();
        document = documentId != null ? repository.findById(documentId) : null;
        if (document == null) {
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        getBinding().toolbar.setTitle(document.getName());
        getBinding().toolbar.setNavigationOnClickListener(
                v -> NavHostFragment.findNavController(this).popBackStack());
        getBinding().toolbar.inflateMenu(R.menu.menu_document_viewer);
        getBinding().toolbar.setOnMenuItemClickListener(this::onMenuItem);

        getBinding().errorView.setTitle(R.string.viewer_error_title);
        getBinding().errorView.setMessage(R.string.viewer_error_message);

        getBinding().recyclerPdf.setLayoutManager(new LinearLayoutManager(requireContext()));
        getBinding().recyclerPdf.setAdapter(pdfAdapter);

        prepareAndRender(document);
    }

    private void prepareAndRender(@NonNull Document doc) {
        getBinding().loadingView.setVisibility(View.VISIBLE);
        int targetWidth = pageTargetWidth();
        File target = new File(new File(requireContext().getCacheDir(), "documents"), doc.getFileName());

        ioExecutor.execute(() -> {
            File file;
            Bitmap singleImage = null;
            List<Bitmap> pdfPages = null;
            try {
                file = SampleDocumentGenerator.writeFile(doc, target);
                if (doc.getFileType().isImage()) {
                    singleImage = BitmapFactory.decodeFile(file.getAbsolutePath());
                } else {
                    pdfPages = renderPdf(file, targetWidth);
                }
            } catch (Exception e) {
                deliverError();
                return;
            }
            File finalFile = file;
            Bitmap finalImage = singleImage;
            List<Bitmap> finalPages = pdfPages;
            mainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                preparedFile = finalFile;
                getBinding().loadingView.setVisibility(View.GONE);
                if (finalImage != null) {
                    getBinding().imageSingle.setVisibility(View.VISIBLE);
                    getBinding().imageSingle.setImageBitmap(finalImage);
                } else if (finalPages != null && !finalPages.isEmpty()) {
                    getBinding().recyclerPdf.setVisibility(View.VISIBLE);
                    pdfAdapter.submit(finalPages);
                } else {
                    getBinding().errorView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    @NonNull
    private List<Bitmap> renderPdf(@NonNull File file, int targetWidth) throws Exception {
        List<Bitmap> pages = new ArrayList<>();
        try (ParcelFileDescriptor pfd =
                     ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer renderer = new PdfRenderer(pfd)) {
            int count = renderer.getPageCount();
            for (int i = 0; i < count; i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                int w = targetWidth;
                int h = Math.max(1, (int) (page.getHeight() * (w / (float) page.getWidth())));
                Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                pages.add(bitmap);
            }
        }
        return pages;
    }

    private void deliverError() {
        mainHandler.post(() -> {
            if (!isAdded()) {
                return;
            }
            getBinding().loadingView.setVisibility(View.GONE);
            getBinding().errorView.setVisibility(View.VISIBLE);
        });
    }

    private int pageTargetWidth() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int padding = (int) (getResources().getDimension(R.dimen.space_sm) * 2);
        return Math.max(320, metrics.widthPixels - padding);
    }

    private boolean onMenuItem(@NonNull android.view.MenuItem item) {
        if (document == null || preparedFile == null) {
            return false;
        }
        int id = item.getItemId();
        if (id == R.id.action_open_external) {
            try {
                FileShareUtils.open(requireContext(), preparedFile, document.getFileType().getMimeType());
            } catch (ActivityNotFoundException e) {
                toast(getString(R.string.documents_error_no_viewer));
            }
            return true;
        } else if (id == R.id.action_download) {
            downloadCopy();
            return true;
        } else if (id == R.id.action_share) {
            try {
                FileShareUtils.share(requireContext(), preparedFile, document.getFileType().getMimeType(),
                        document.getName(), getString(R.string.documents_action_share));
            } catch (ActivityNotFoundException e) {
                toast(getString(R.string.documents_error_no_viewer));
            }
            return true;
        }
        return false;
    }

    private void downloadCopy() {
        Document doc = document;
        if (doc == null) {
            return;
        }
        File dir = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Documents");
        File target = new File(dir, doc.getFileName());
        ioExecutor.execute(() -> {
            String path;
            try {
                path = SampleDocumentGenerator.writeFile(doc, target).getAbsolutePath();
            } catch (Exception e) {
                path = null;
            }
            String finalPath = path;
            mainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                toast(finalPath == null
                        ? getString(R.string.documents_error_open)
                        : getString(R.string.documents_downloaded_format, finalPath));
            });
        });
    }

    private void toast(@NonNull String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }
}
