package com.triotech.hrms.ui.documents;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.triotech.hrms.databinding.ItemPdfPageBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the pages of a PDF (already rasterised to {@link Bitmap}s) into a
 * vertically scrolling list, each page in a {@link com.triotech.hrms.ui.components.ZoomableImageView}
 * so it can be pinch/double-tap zoomed while the list handles scrolling between pages.
 */
public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageViewHolder> {

    private final List<Bitmap> pages = new ArrayList<>();

    public void submit(@NonNull List<Bitmap> newPages) {
        pages.clear();
        pages.addAll(newPages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PageViewHolder(
                ItemPdfPageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        holder.bind(pages.get(position));
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        private final ItemPdfPageBinding binding;

        PageViewHolder(@NonNull ItemPdfPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Bitmap page) {
            // Give the page item a height matching its rendered bitmap so the list scrolls page-by-page.
            ViewGroup.LayoutParams params = binding.imagePdfPage.getLayoutParams();
            params.height = page.getHeight();
            binding.imagePdfPage.setLayoutParams(params);
            binding.imagePdfPage.setImageBitmap(page);
        }
    }
}
