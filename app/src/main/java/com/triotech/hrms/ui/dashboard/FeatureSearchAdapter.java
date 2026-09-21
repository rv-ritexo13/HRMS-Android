package com.triotech.hrms.ui.dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.triotech.hrms.databinding.ItemSearchResultBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Backs the Home search bar's dropdown. Filters the fixed set of app features by
 * a substring match against each feature's label + synonyms, so "money" finds
 * Salary and "goals" finds Performance. Selecting a row opens that feature.
 */
class FeatureSearchAdapter extends ArrayAdapter<FeatureSearchTarget> {

    private final List<FeatureSearchTarget> all;
    private final List<FeatureSearchTarget> filtered = new ArrayList<>();

    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(@Nullable CharSequence constraint) {
            List<FeatureSearchTarget> matches = new ArrayList<>();
            String query = constraint == null ? "" : constraint.toString().trim().toLowerCase(Locale.getDefault());
            if (!query.isEmpty()) {
                for (FeatureSearchTarget target : all) {
                    if (target.matches(query)) {
                        matches.add(target);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = matches;
            results.count = matches.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(@Nullable CharSequence constraint, @NonNull FilterResults results) {
            filtered.clear();
            if (results.values instanceof List) {
                filtered.addAll((List<FeatureSearchTarget>) results.values);
            }
            notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(@Nullable Object resultValue) {
            // The field is cleared on selection, so no completion text is needed.
            return "";
        }
    };

    FeatureSearchAdapter(@NonNull Context context, @NonNull List<FeatureSearchTarget> targets) {
        super(context, 0);
        this.all = new ArrayList<>(targets);
    }

    @Override
    public int getCount() {
        return filtered.size();
    }

    @Nullable
    @Override
    public FeatureSearchTarget getItem(int position) {
        return filtered.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return filter;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ItemSearchResultBinding binding;
        if (convertView == null) {
            binding = ItemSearchResultBinding.inflate(LayoutInflater.from(getContext()), parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding);
        } else {
            binding = (ItemSearchResultBinding) convertView.getTag();
        }
        FeatureSearchTarget target = filtered.get(position);
        binding.imageSearchIcon.setImageResource(target.getIconRes());
        binding.textSearchTitle.setText(target.getTitleRes());
        binding.textSearchSubtitle.setText(target.getSubtitleRes());
        return convertView;
    }
}
