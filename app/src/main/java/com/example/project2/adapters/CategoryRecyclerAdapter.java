package com.example.project2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project2.R;
import com.example.project2.fragments.BaseListFragment;
import com.example.project2.models.Category;
import com.example.project2.utils.CategoryManager;

import java.util.List;

public class CategoryRecyclerAdapter extends RecyclerView.Adapter<CategoryRecyclerAdapter.ViewHolder>
        implements BaseListFragment.UpdatableAdapter<Category> {

    public interface OnCategoryClickListener {
        void onClick(Category category);
    }

    public interface OnCategoryLongClickListener {
        void onLongClick(Category category);
    }

    private final Context context;
    private List<Category> categories;
    private final OnCategoryClickListener clickListener;
    private final OnCategoryLongClickListener longClickListener;
    private final CategoryManager categoryManager;

    public CategoryRecyclerAdapter(Context context, List<Category> categories,
                                   OnCategoryClickListener clickListener,
                                   OnCategoryLongClickListener longClickListener) {
        this.context = context;
        this.categories = categories;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.categoryManager = CategoryManager.getInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.name.setText(category.getName());
        int count = categoryManager.getAppsCountInCategory(category.getId());
        holder.count.setText(count + " прил.");
        holder.colorIndicator.setBackgroundColor(category.getColor());

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(category);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onLongClick(category);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    @Override
    public void updateData(List<Category> newData) {
        this.categories = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, count;
        View colorIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.category_name);
            count = itemView.findViewById(R.id.category_count);
            colorIndicator = itemView.findViewById(R.id.color_indicator);
        }
    }
}