package com.example.project2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.project2.models.AppInfo;
import com.example.project2.models.Category;
import com.example.project2.utils.CategoryManager;

import java.util.List;

public class CategorySelectionAdapter extends BaseAdapter {
    private Context context;
    private List<Category> categories;
    private AppInfo app;
    private LayoutInflater inflater;
    private CategoryManager categoryManager;

    public CategorySelectionAdapter(Context context, List<Category> categories, AppInfo app) {
        this.context = context;
        this.categories = categories;
        this.app = app;
        this.inflater = LayoutInflater.from(context);
        this.categoryManager = CategoryManager.getInstance(context);
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
            convertView = inflater.inflate(R.layout.item_category_selection, parent, false);
            holder = new ViewHolder();
            holder.checkBox = convertView.findViewById(R.id.category_checkbox);
            holder.name = convertView.findViewById(R.id.category_name);
            holder.colorIndicator = convertView.findViewById(R.id.color_indicator);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Category category = categories.get(position);

        holder.name.setText(category.getName());
        holder.colorIndicator.setBackgroundColor(category.getColor());

        // Проверяем, выбрана ли категория для этого приложения
        boolean isChecked = categoryManager.isAppInUserCategory(app.getPackageName(), category.getId());
        holder.checkBox.setChecked(isChecked);

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked1) -> {
            if (isChecked1) {
                categoryManager.addAppToCategory(app.getPackageName(), category.getId());
            } else {
                categoryManager.removeAppFromCategory(app.getPackageName(), category.getId());
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        CheckBox checkBox;
        TextView name;
        View colorIndicator;
    }
}