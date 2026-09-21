package com.triotech.hrms.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.triotech.hrms.data.model.NotificationItem;
import com.triotech.hrms.databinding.ItemNotificationBinding;

/** Feeds the Home dashboard's "Recent notifications" section. */
public class NotificationAdapter extends ListAdapter<NotificationItem, NotificationAdapter.ViewHolder> {

    public NotificationAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding =
                ItemNotificationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;

        ViewHolder(@NonNull ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull NotificationItem item) {
            binding.textNotificationTitle.setText(item.getTitle());
            binding.textNotificationMessage.setText(item.getMessage());
            binding.textNotificationTime.setText(item.getTimeLabel());
            binding.dotUnread.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
        }
    }

    private static final DiffUtil.ItemCallback<NotificationItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<NotificationItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull NotificationItem oldItem, @NonNull NotificationItem newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull NotificationItem oldItem, @NonNull NotificationItem newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getMessage().equals(newItem.getMessage())
                            && oldItem.isRead() == newItem.isRead();
                }
            };
}
