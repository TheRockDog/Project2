package com.example.project2;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.project2.models.AppInfo;
import com.example.project2.models.Category;
import com.example.project2.utils.CategoryManager;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends BaseAdapter {
    private Context context;
    private List<AppInfo> apps;
    private LayoutInflater inflater;
    private PackageManager packageManager;
    private CategoryManager categoryManager;

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

        convertView.setOnLongClickListener(v -> {
            showCategorySelectionDialog(app);
            return true;
        });

        return convertView;
    }

    // Диалог выбора категорий
    private void showCategorySelectionDialog(AppInfo app) {
        List<Category> categories = categoryManager.getAllCategories();

        // Текущее состояние
        boolean[] originalChecked = new boolean[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            originalChecked[i] = app.isInUserCategory(categories.get(i).getId());
        }

        // Временное состояние
        boolean[] tempChecked = originalChecked.clone();

        // Список элементов (категории + пункт создания)
        List<Object> items = new ArrayList<>(categories);
        items.add("CREATE");

        ListView listView = new ListView(context);
        listView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return items.size();
            }

            @Override
            public Object getItem(int position) {
                return items.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.item_category_select, parent, false);
                }

                View indicator = convertView.findViewById(R.id.state_indicator);
                TextView nameView = convertView.findViewById(R.id.category_name);

                if (position < categories.size()) {
                    Category cat = categories.get(position);
                    nameView.setText(cat.getName());
                    indicator.setVisibility(View.VISIBLE);
                    indicator.setBackgroundColor(tempChecked[position] ? 0xFF4CAF50 : 0xFF9E9E9E);
                } else {
                    nameView.setText("Создать новую категорию");
                    indicator.setVisibility(View.GONE);
                }
                return convertView;
            }
        });

        // Строим диалог
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Категории для " + app.getAppName());
        builder.setView(listView);
        builder.setPositiveButton("Готово", (dialog, which) -> {
            int added = 0, removed = 0;
            for (int i = 0; i < categories.size(); i++) {
                if (tempChecked[i] != originalChecked[i]) {
                    Category cat = categories.get(i);
                    if (tempChecked[i]) {
                        categoryManager.addAppToCategory(app.getPackageName(), cat.getId());
                        app.addToUserCategory(cat.getId());
                        added++;
                    } else {
                        categoryManager.removeAppFromCategory(app.getPackageName(), cat.getId());
                        app.removeFromUserCategory(cat.getId());
                        removed++;
                    }
                }
            }
            if (added > 0 || removed > 0) {
                String message;
                if (added > 0 && removed > 0) {
                    message = "Изменения сохранены";
                } else if (added > 0) {
                    message = "Приложение добавлено в категории";
                } else {
                    message = "Приложение удалено из категорий";
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
            notifyDataSetChanged();
        });
        builder.setNegativeButton("Отмена", null);

        AlertDialog dialog = builder.create();

        // Обработка кликов по элементам списка
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < categories.size()) {
                // Переключаем состояние категории
                tempChecked[position] = !tempChecked[position];
                // Обновляем индикатор
                View indicator = view.findViewById(R.id.state_indicator);
                if (indicator != null) {
                    indicator.setBackgroundColor(tempChecked[position] ? 0xFF4CAF50 : 0xFF9E9E9E);
                }
            } else {
                // Создание новой категории – закрываем текущий диалог
                dialog.dismiss(); // закрываем диалог выбора

                CategoryNameDialog.newInstance(name -> {
                    Category newCategory = categoryManager.createCategory(name);
                    if (newCategory == null) {
                        Toast.makeText(context, "Не удалось создать категорию", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Добавляем текущее приложение в новую категорию
                    categoryManager.addAppToCategory(app.getPackageName(), newCategory.getId());
                    app.addToUserCategory(newCategory.getId());

                    // Открываем редактор категории
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).runOnUiThread(() ->
                                CategoryEditDialog.newInstance(newCategory)
                                        .show(((MainActivity) context).getSupportFragmentManager(), "edit_category")
                        );
                    }
                }).show(((MainActivity) context).getSupportFragmentManager(), "name_dialog");
            }
        });

        dialog.show();
    }

    public void updateApps(List<AppInfo> newApps) {
        this.apps = newApps;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
    }
}