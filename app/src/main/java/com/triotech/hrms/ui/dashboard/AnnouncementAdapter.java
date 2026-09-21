package com.triotech.hrms.ui.dashboard;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.triotech.hrms.data.model.Announcement;
import com.triotech.hrms.databinding.ItemAnnouncementBinding;

/** Feeds the Home dashboard's "Recent announcements" section. */
public class AnnouncementAdapter extends ListAdapter<Announcement, AnnouncementAdapter.ViewHolder> {

    public AnnouncementAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAnnouncementBinding binding =
                ItemAnnouncementBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAnnouncementBinding binding;

        ViewHolder(@NonNull ItemAnnouncementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Announcement item) {
            binding.textAnnouncementTitle.setText(item.getTitle());
            binding.textAnnouncementBody.setText(item.getBody());
            binding.textAnnouncementPosted.setText(item.getPostedLabel());
        }
    }

    private static final DiffUtil.ItemCallback<Announcement> DIFF_CALLBACK = new DiffUtil.ItemCallback<Announcement>() {
        @Override
        public boolean areItemsTheSame(@NonNull Announcement oldItem, @NonNull Announcement newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Announcement oldItem, @NonNull Announcement newItem) {
            return oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getBody().equals(newItem.getBody())
                    && oldItem.getPostedLabel().equals(newItem.getPostedLabel());
        }
    };
}
