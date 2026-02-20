package com.example.project2;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.project2.models.AppInfo;
import com.example.project2.models.Category;
import com.example.project2.utils.AppManager;
import com.example.project2.utils.CategoryManager;

import java.util.ArrayList;
import java.util.List;

public class CategoryEditDialog extends DialogFragment {

    private static final String ARG_CATEGORY = "category";
    private static final String ARG_IS_NEW = "is_new";
    private static final String ARG_CATEGORY_NAME = "category_name";

    private Category category; // для редактирования
    private boolean isNewCategory;
    private String newCategoryName; // для создания

    private CategoryManager categoryManager;
    private List<AppInfo> allApps;
    private List<AppItem> appItems;
    private AppItemAdapter adapter;
    private EditText editName;
    private TextView errorText;
    private ListView listView;
    private Button btnSave, btnCancel;
    private CategoryEditListener listener;

    public interface CategoryEditListener {
        void onCategoryEdited();
    }

    // Для редактирования существующей
    public static CategoryEditDialog newInstance(Category category) {
        CategoryEditDialog dialog = new CategoryEditDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CATEGORY, category);
        args.putBoolean(ARG_IS_NEW, false);
        dialog.setArguments(args);
        return dialog;
    }

    // Для создания новой
    public static CategoryEditDialog newInstanceForCreate(String name) {
        CategoryEditDialog dialog = new CategoryEditDialog();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_NAME, name);
        args.putBoolean(ARG_IS_NEW, true);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isNewCategory = getArguments().getBoolean(ARG_IS_NEW, false);
            if (!isNewCategory) {
                // Безопасное извлечение Parcelable (API 33+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    category = getArguments().getParcelable(ARG_CATEGORY, Category.class);
                } else {
                    category = getArguments().getParcelable(ARG_CATEGORY);
                }
            } else {
                newCategoryName = getArguments().getString(ARG_CATEGORY_NAME);
            }
        }
        categoryManager = CategoryManager.getInstance(requireContext());
        if (getParentFragment() instanceof CategoryEditListener) {
            listener = (CategoryEditListener) getParentFragment();
        } else if (getActivity() instanceof CategoryEditListener) {
            listener = (CategoryEditListener) getActivity();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_category_edit, null);
        builder.setView(view);

        editName = view.findViewById(R.id.edit_category_name);
        errorText = view.findViewById(R.id.error_text);
        listView = view.findViewById(R.id.list_apps);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);

        if (isNewCategory) {
            editName.setText(newCategoryName);
        } else {
            editName.setText(category.getName());
        }

        // Асинхронная загрузка приложений
        AppManager.getAllAppsAsync(requireContext(), apps -> {
            allApps = apps;
            prepareAppItems();
            adapter = new AppItemAdapter(requireContext(), appItems,
                    isNewCategory ? new ArrayList<>() : category.getPackageNames());
            listView.setAdapter(adapter);
        });

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                errorText.setVisibility(View.VISIBLE);
                errorText.setText("Название не может быть пустым");
                return;
            } else {
                errorText.setVisibility(View.GONE);
            }

            Category workingCategory;
            if (isNewCategory) {
                workingCategory = categoryManager.createCategory(name);
                if (workingCategory == null) {
                    Toast.makeText(getContext(), "Не удалось создать категорию", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                workingCategory = category;
                workingCategory.setName(name);
                categoryManager.updateCategory(workingCategory);
            }

            // Применяем изменения
            for (AppItem item : appItems) {
                boolean currentlyInCategory = workingCategory.containsPackage(item.packageName);
                if (item.state == 1 && !currentlyInCategory) {
                    categoryManager.addAppToCategory(item.packageName, workingCategory.getId());
                } else if (item.state == 2 && currentlyInCategory) {
                    categoryManager.removeAppFromCategory(item.packageName, workingCategory.getId());
                } else if (item.state == 0 && currentlyInCategory) {
                    categoryManager.removeAppFromCategory(item.packageName, workingCategory.getId());
                }
            }

            Toast.makeText(getContext(), "Категория сохранена", Toast.LENGTH_SHORT).show();
            WidgetProvider.updateAllWidgets(requireContext());

            if (listener != null) {
                listener.onCategoryEdited();
            }

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }

    // Подготовка списка приложений
    private void prepareAppItems() {
        appItems = new ArrayList<>();
        List<String> currentPackages = (isNewCategory || category == null) ? new ArrayList<>() : category.getPackageNames();

        for (AppInfo app : allApps) {
            AppItem item = new AppItem();
            item.packageName = app.getPackageName();
            item.appName = app.getAppName();
            item.icon = app.getIcon();
            if (currentPackages.contains(item.packageName)) {
                item.state = 1; // зелёный
                item.originalInCategory = true;
            } else {
                item.state = 0; // серый
                item.originalInCategory = false;
            }
            appItems.add(item);
        }
    }

    // Внутренний класс
    private static class AppItem {
        String packageName;
        String appName;
        Drawable icon;
        int state; // 0-серый, 1-зелёный, 2-красный
        boolean originalInCategory;
    }

    // Адаптер
    private class AppItemAdapter extends BaseAdapter {
        private Context context;
        private List<AppItem> items;
        private LayoutInflater inflater;
        private List<String> originalPackages;

        AppItemAdapter(Context context, List<AppItem> items, List<String> originalPackages) {
            this.context = context;
            this.items = items;
            this.inflater = LayoutInflater.from(context);
            this.originalPackages = originalPackages;
        }

        @Override
        public int getCount() { return items.size(); }

        @Override
        public Object getItem(int position) { return items.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_category_app, parent, false);
                holder = new ViewHolder();
                holder.icon = convertView.findViewById(R.id.app_icon);
                holder.name = convertView.findViewById(R.id.app_name);
                holder.stateIndicator = convertView.findViewById(R.id.state_indicator);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            AppItem item = items.get(position);
            holder.icon.setImageDrawable(item.icon);
            holder.name.setText(item.appName);

            int color;
            switch (item.state) {
                case 1: color = 0xFF4CAF50; break;
                case 2: color = 0xFFF44336; break;
                default: color = 0xFF9E9E9E;
            }
            holder.stateIndicator.setBackgroundColor(color);

            convertView.setOnClickListener(v -> {
                if (item.originalInCategory) {
                    if (item.state == 1) item.state = 2;
                    else if (item.state == 2) item.state = 1;
                } else {
                    if (item.state == 0) item.state = 1;
                    else if (item.state == 1) item.state = 0;
                }
                notifyDataSetChanged();
            });

            return convertView;
        }

        class ViewHolder {
            ImageView icon;
            TextView name;
            View stateIndicator;
        }
    }
}