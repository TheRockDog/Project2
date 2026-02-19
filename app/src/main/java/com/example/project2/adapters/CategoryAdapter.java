package com.example.project2.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.project2.R;
import com.example.project2.models.Category;
import com.example.project2.utils.CategoryManager;

import java.util.List;

public class CategoryAdapter extends BaseAdapter {
    private Context context;
    private List<Category> categories;
    private LayoutInflater inflater;
    private OnCategoryClickListener clickListener;
    private OnCategoryLongClickListener longClickListener;

    public interface OnCategoryClickListener {
        void onClick(Category category);
    }

    public interface OnCategoryLongClickListener {
        void onLongClick(Category category);
    }

    public CategoryAdapter(Context context, List<Category> categories,
                           OnCategoryClickListener clickListener,
                           OnCategoryLongClickListener longClickListener) {
        this.context = context;
        this.categories = categories;
        this.inflater = LayoutInflater.from(context);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Object getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return categories.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_category, parent, false);
            holder = new ViewHolder();
            holder.name = convertView.findViewById(R.id.category_name);
            holder.count = convertView.findViewById(R.id.category_count);
            holder.colorIndicator = convertView.findViewById(R.id.color_indicator);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Category category = categories.get(position);
        holder.name.setText(category.getName());
        int count = CategoryManager.getInstance(context).getAppsCountInCategory(category.getId());
        holder.count.setText(count + " прил.");
        holder.colorIndicator.setBackgroundColor(category.getColor());

        convertView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(category);
        });

        convertView.setOnLongClickListener(v -> {
            if (longClickListener != null) longClickListener.onLongClick(category);
            return true;
        });

        return convertView;
    }

    public void updateCategories(List<Category> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        TextView name;
        TextView count;
        View colorIndicator;
    }
}