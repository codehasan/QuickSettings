package io.github.codehasan.quicksettings.ui.adapter;

import static io.github.codehasan.quicksettings.util.NullSafety.isNullOrEmpty;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import io.github.codehasan.quicksettings.R;
import io.github.codehasan.quicksettings.ui.model.ServiceItem;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> implements Filterable {

    private final List<ServiceItem> masterList;
    private final List<ServiceItem> filteredList;

    public ServiceAdapter(List<ServiceItem> list) {
        this.masterList = list;
        this.filteredList = new ArrayList<>(list);
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.service_info, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        ServiceItem item = filteredList.get(position);

        if (!isNullOrEmpty(item.reason)) {
            holder.tvReason.setText(item.reason);
            holder.tvReason.setVisibility(View.VISIBLE);
        } else {
            holder.tvReason.setVisibility(View.GONE);
        }
        holder.ivIcon.setImageResource(item.icon);
        holder.tvTitle.setText(item.title);
        holder.tvDescription.setText(item.description);

        // Remove listener temporarily to avoid triggering it during recycling
        holder.switchService.setOnCheckedChangeListener(null);
        holder.switchService.setChecked(item.isActive);

        holder.switchService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Context context = holder.switchService.getContext();
            PackageManager pm = context.getPackageManager();
            int newState = isChecked ?
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            pm.setComponentEnabledSetting(
                    new ComponentName(context.getPackageName(), item.serviceClass),
                    newState,
                    PackageManager.DONT_KILL_APP
            );
            item.isActive = isChecked;
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<ServiceItem> resultsList = new ArrayList<>();

                if (isNullOrEmpty(constraint)) {
                    resultsList.addAll(masterList);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (ServiceItem item : masterList) {
                        if (item.title.toLowerCase().contains(filterPattern) ||
                                item.description.toLowerCase().contains(filterPattern)) {
                            resultsList.add(item);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = resultsList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList.clear();
                filteredList.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }

    public static class ServiceViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView tvReason, tvTitle, tvDescription;
        ShapeableImageView ivIcon;
        MaterialSwitch switchService;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvReason = itemView.findViewById(R.id.tv_reason);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDescription = itemView.findViewById(R.id.tv_description);
            switchService = itemView.findViewById(R.id.switch_service);
        }
    }
}