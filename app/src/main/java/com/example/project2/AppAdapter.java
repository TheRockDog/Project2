package com.example.project2;

import android.widget.Button;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.project2.models.AppInfo;
import com.example.project2.models.Category;
import com.example.project2.utils.CategoryManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
    public int getCount() {
        return apps.size();
    }

    @Override
    public Object getItem(int position) {
        return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_app, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.app_icon);
            holder.name = convertView.findViewById(R.id.app_name);
            holder.categoryIndicator = convertView.findViewById(R.id.category_indicator);
            holder.autoCategoryBadge = convertView.findViewById(R.id.auto_category_badge);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppInfo app = apps.get(position);
        holder.icon.setImageDrawable(app.getIcon());
        holder.name.setText(app.getAppName());

        // Показываем индикатор пользовательских категорий
        int userCategoriesCount = app.getUserCategoryIds().size();
        if (userCategoriesCount > 0) {
            holder.categoryIndicator.setVisibility(View.VISIBLE);
            holder.categoryIndicator.setText("📁 " + userCategoriesCount);

            // Получаем первую категорию для цвета индикатора
            if (!app.getUserCategoryIds().isEmpty()) {
                Category firstCategory = categoryManager.getCategory(app.getUserCategoryIds().get(0));
                if (firstCategory != null) {
                    holder.categoryIndicator.setBackgroundColor(firstCategory.getColor());
                }
            }
        } else {
            holder.categoryIndicator.setVisibility(View.GONE);
        }

        // Показываем автоматическую категорию
        String autoCategory = app.getAutoCategory();
        if (!autoCategory.equals("Other")) {
            holder.autoCategoryBadge.setVisibility(View.VISIBLE);
            holder.autoCategoryBadge.setText(getCategoryBadgeText(autoCategory));
            setCategoryBadgeColor(holder.autoCategoryBadge, autoCategory);
        } else {
            holder.autoCategoryBadge.setVisibility(View.GONE);
        }

        // Открытие приложения по клику
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

        // Выбор категорий по долгому нажатию
        convertView.setOnLongClickListener(v -> {
            showCategorySelectionDialog(app);
            return true;
        });

        return convertView;
    }

    private String getCategoryBadgeText(String category) {
        switch (category) {
            case "Games": return "Игры";
            case "Social": return "Соцсети";
            case "Work": return "Работа";
            default: return "Другое";
        }
    }

    private void setCategoryBadgeColor(TextView badge, String category) {
        int color;
        switch (category) {
            case "Games":
                color = 0xFF4CAF50; // Зеленый
                break;
            case "Social":
                color = 0xFF2196F3; // Синий
                break;
            case "Work":
                color = 0xFFFF9800; // Оранжевый
                break;
            default:
                color = 0xFF9E9E9E; // Серый
        }
        badge.setBackgroundColor(color);
        badge.setTextColor(0xFFFFFFFF);
    }

    private void showCategorySelectionDialog(AppInfo app) {
        List<Category> categories = categoryManager.getAllCategories();

        String[] items = new String[categories.size() + 1];
        boolean[] checked = new boolean[categories.size() + 1];

        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            items[i] = category.getName();
            checked[i] = app.isInUserCategory(category.getId());
        }
        items[categories.size()] = "➕ Создать новую категорию";
        checked[categories.size()] = false;

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Категории для " + app.getAppName())
                .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
                    if (which == categories.size()) {
                        // Создание новой категории
                        showCreateCategoryDialog(app);
                    } else {
                        Category category = categories.get(which);
                        if (isChecked) {
                            categoryManager.addAppToCategory(app.getPackageName(), category.getId());
                            Toast.makeText(context,
                                    "Добавлено в \"" + category.getName() + "\"",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            categoryManager.removeAppFromCategory(app.getPackageName(), category.getId());
                            Toast.makeText(context,
                                    "Удалено из \"" + category.getName() + "\"",
                                    Toast.LENGTH_SHORT).show();
                        }
                        notifyDataSetChanged();
                    }
                })
                .setPositiveButton("Готово", (dialog, which) -> {
                    notifyDataSetChanged();
                })
                .setNeutralButton("Управление", (dialog, which) -> {
                    showCategoryManagementDialog();
                })
                .show();
    }

    private void showCreateCategoryDialog(AppInfo app) {
        // Создаем кастомный диалог
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        View view = inflater.inflate(R.layout.dialog_create_category, null);
        builder.setView(view);

        EditText editName = view.findViewById(R.id.edit_category_name);

        // Явно указываем тип для кнопок
        android.widget.Button btnCreate = view.findViewById(R.id.btn_create);
        android.widget.Button btnCancel = view.findViewById(R.id.btn_cancel);

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(context, "Введите название категории", Toast.LENGTH_SHORT).show();
                    return;
                }

                Category category = categoryManager.createCategory(name);
                categoryManager.addAppToCategory(app.getPackageName(), category.getId());
                Toast.makeText(context,
                        "✅ Категория \"" + name + "\" создана и добавлена",
                        Toast.LENGTH_SHORT).show();
                notifyDataSetChanged();
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private void showCategoryManagementDialog() {
        List<Category> categories = categoryManager.getAllCategories();

        if (categories.isEmpty()) {
            Toast.makeText(context, "Нет созданных категорий", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            items[i] = category.getName() + " (" +
                    categoryManager.getAppsCountInCategory(category.getId()) + " прил.)";
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Управление категориями")
                .setItems(items, (dialog, which) -> {
                    showCategoryOptionsDialog(categories.get(which));
                })
                .setPositiveButton("Закрыть", null)
                .show();
    }

    private void showCategoryOptionsDialog(Category category) {
        String[] options = {"✏️ Переименовать", "🎨 Изменить цвет", "🗑️ Удалить"};

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(category.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showRenameCategoryDialog(category);
                            break;
                        case 1:
                            showChangeColorDialog(category);
                            break;
                        case 2:
                            showDeleteCategoryDialog(category);
                            break;
                    }
                })
                .show();
    }

    private void showRenameCategoryDialog(Category category) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        View view = inflater.inflate(R.layout.dialog_create_category, null);
        builder.setView(view);

        EditText editName = view.findViewById(R.id.edit_category_name);
        editName.setText(category.getName());
        editName.setHint("Новое название");

        android.widget.Button btnCreate = view.findViewById(R.id.btn_create);
        android.widget.Button btnCancel = view.findViewById(R.id.btn_cancel);

        // Меняем текст кнопки
        btnCreate.setText("Сохранить");

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newName = editName.getText().toString().trim();
                if (!newName.isEmpty()) {
                    category.setName(newName);
                    categoryManager.updateCategory(category);
                    Toast.makeText(context, "✅ Категория переименована", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                    dialog.dismiss();
                } else {
                    Toast.makeText(context, "Введите название категории", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private void showChangeColorDialog(Category category) {
        // Простой выбор цвета
        int[] colors = {
                0xFFF44336, // Красный
                0xFFFF9800, // Оранжевый
                0xFFFFEB3B, // Желтый
                0xFF4CAF50, // Зеленый
                0xFF2196F3, // Синий
                0xFF9C27B0, // Фиолетовый
                0xFFFF4081, // Розовый
                0xFF795548  // Коричневый
        };

        String[] colorNames = {"Красный", "Оранжевый", "Желтый", "Зеленый",
                "Синий", "Фиолетовый", "Розовый", "Коричневый"};

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Выберите цвет для \"" + category.getName() + "\"")
                .setItems(colorNames, (dialog, which) -> {
                    category.setColor(colors[which]);
                    categoryManager.updateCategory(category);
                    Toast.makeText(context, "✅ Цвет изменен", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                })
                .show();
    }

    private void showDeleteCategoryDialog(Category category) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Удалить категорию")
                .setMessage("Вы уверены, что хотите удалить категорию \"" + category.getName() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    categoryManager.deleteCategory(category.getId());
                    Toast.makeText(context, "🗑️ Категория удалена", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    public void updateApps(List<AppInfo> newApps) {
        this.apps = newApps;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        ImageView icon;
        TextView name;
        TextView categoryIndicator;
        TextView autoCategoryBadge;
    }
}