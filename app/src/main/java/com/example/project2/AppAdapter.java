package com.example.project2;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.project2.models.AppInfo;
import com.example.project2.models.Category;
import com.example.project2.utils.CategoryManager;

import java.util.List;

public class AppAdapter extends BaseAdapter {
    private Context context;
    private List<AppInfo> apps;
    private LayoutInflater inflater;
    private PackageManager packageManager;
    private CategoryManager categoryManager;

    // Конструктор
    public AppAdapter(Context context, List<AppInfo> apps) {
        this.context = context;
        this.apps = apps;
        this.inflater = LayoutInflater.from(context);
        this.packageManager = context.getPackageManager();
        this.categoryManager = CategoryManager.getInstance(context);
    }

    @Override
    public int getCount() { return apps.size(); }

    @Override
    public Object getItem(int position) { return apps.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    // Заполнение элемента
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_app, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.app_icon);
            holder.name = convertView.findViewById(R.id.app_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppInfo app = apps.get(position);
        holder.icon.setImageDrawable(app.getIcon());
        holder.name.setText(app.getAppName());

        // Запуск приложения
        convertView.setOnClickListener(v -> {
            try {
                Intent launchIntent = packageManager.getLaunchIntentForPackage(app.getPackageName());
                if (launchIntent != null) {
                    context.startActivity(launchIntent);
                } else {
                    Toast.makeText(context, "Не удалось открыть приложение", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(context, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Долгое нажатие — категории
        convertView.setOnLongClickListener(v -> {
            showCategorySelectionDialog(app);
            return true;
        });

        return convertView;
    }

    // Диалог выбора категорий
    private void showCategorySelectionDialog(AppInfo app) {
        List<Category> categories = categoryManager.getAllCategories();

        String[] items = new String[categories.size() + 1];
        boolean[] checked = new boolean[categories.size() + 1];

        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            items[i] = category.getName();
            checked[i] = app.isInUserCategory(category.getId());
        }
        items[categories.size()] = "Создать новую категорию";
        checked[categories.size()] = false;

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Категории для " + app.getAppName())
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
                    if (which == categories.size()) {
                        // Создание новой категории
                        dialog.dismiss();
                        CategoryNameDialog.newInstance(name -> {
                            Category newCategory = categoryManager.createCategory(name);
                            if (newCategory == null) {
                                Toast.makeText(context, "Не удалось создать категорию", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // Открыть редактор новой категории
                            CategoryEditDialog.newInstance(newCategory).show(((MainActivity) context).getSupportFragmentManager(), "edit_category");
                        }).show(((MainActivity) context).getSupportFragmentManager(), "name_dialog");
                    } else {
                        Category category = categories.get(which);
                        if (isChecked) {
                            categoryManager.addAppToCategory(app.getPackageName(), category.getId());
                            app.addToUserCategory(category.getId());
                            Toast.makeText(context, "Добавлено в \"" + category.getName() + "\"", Toast.LENGTH_SHORT).show();
                        } else {
                            categoryManager.removeAppFromCategory(app.getPackageName(), category.getId());
                            app.removeFromUserCategory(category.getId());
                            Toast.makeText(context, "Удалено из \"" + category.getName() + "\"", Toast.LENGTH_SHORT).show();
                        }
                        notifyDataSetChanged();
                    }
                })
                .setPositiveButton("Готово", (dialog, which) -> notifyDataSetChanged())
                .setNeutralButton("Управление", (dialog, which) -> {
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).switchToCategoriesTab();
                    }
                })
                .show();
    }

    // Обновление списка
    public void updateApps(List<AppInfo> newApps) {
        this.apps = newApps;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
    }
}