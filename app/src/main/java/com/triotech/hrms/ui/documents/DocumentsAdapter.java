package com.triotech.hrms.ui.documents;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.triotech.hrms.R;
import com.triotech.hrms.core.util.DateUtils;
import com.triotech.hrms.data.model.Document;
import com.triotech.hrms.databinding.ItemDocumentBinding;
import com.triotech.hrms.databinding.ItemDocumentHeaderBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the Documents screen. Renders a flat list mixing
 * category headers ({@link String}) and {@link Document} rows via two view types.
 * The catalog is static, so a plain adapter with full redraw is sufficient.
 */
public class DocumentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_DOCUMENT = 1;

    /** Header rows are Strings; document rows are Documents. */
    private final List<Object> items = new ArrayList<>();
    @Nullable private Listener listener;

    public interface Listener {
        void onView(@NonNull Document document);

        void onDownload(@NonNull Document document);

        void onShare(@NonNull Document document);
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void submit(@NonNull List<Object> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_DOCUMENT;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(ItemDocumentHeaderBinding.inflate(inflater, parent, false));
        }
        return new DocumentViewHolder(ItemDocumentBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) item);
        } else {
            ((DocumentViewHolder) holder).bind((Document) item, listener);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemDocumentHeaderBinding binding;

        HeaderViewHolder(@NonNull ItemDocumentHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull String title) {
            ((android.widget.TextView) binding.getRoot()).setText(title);
        }
    }

    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        private final ItemDocumentBinding binding;

        DocumentViewHolder(@NonNull ItemDocumentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Document document, @Nullable Listener listener) {
            binding.imageDocIcon.setImageResource(
                    document.getFileType().isImage() ? R.drawable.ic_image : R.drawable.ic_picture_as_pdf);
            binding.textDocName.setText(document.getName());
            binding.textDocMeta.setText(binding.getRoot().getContext().getString(
                    R.string.documents_meta_format,
                    document.getFileType().getLabel(),
                    DateUtils.formatLongDate(document.getDateMillis()),
                    document.getSizeLabel()));

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onView(document);
                }
            });
            binding.buttonDocDownload.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDownload(document);
                }
            });
            binding.buttonDocShare.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onShare(document);
                }
            });
        }
    }
}
