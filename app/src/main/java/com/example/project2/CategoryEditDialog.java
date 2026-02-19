package com.example.project2;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
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

    private Category category;
    private CategoryManager categoryManager;
    private List<AppInfo> allApps;
    private List<AppItem> appItems;
    private AppItemAdapter adapter;
    private EditText editName;
    private TextView errorText;
    private ListView listView;
    private Button btnSave, btnCancel;

    public static CategoryEditDialog newInstance(Category category) {
        CategoryEditDialog dialog = new CategoryEditDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CATEGORY, category);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = (Category) getArguments().getSerializable(ARG_CATEGORY);
        }
        categoryManager = CategoryManager.getInstance(requireContext());
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

        editName.setText(category.getName());

        // Асинхронная загрузка приложений
        AppManager.getAllAppsAsync(requireContext(), apps -> {
            allApps = apps;
            prepareAppItems();
            adapter = new AppItemAdapter(requireContext(), appItems);
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

            category.setName(name);
            categoryManager.updateCategory(category);

            for (AppItem item : appItems) {
                boolean currentlyInCategory = category.containsPackage(item.packageName);
                if (item.state == 1 && !currentlyInCategory) {
                    categoryManager.addAppToCategory(item.packageName, category.getId());
                } else if (item.state == 2 && currentlyInCategory) {
                    categoryManager.removeAppFromCategory(item.packageName, category.getId());
                }
            }

            Toast.makeText(getContext(), "Категория обновлена", Toast.LENGTH_SHORT).show(); // убран эмодзи
            WidgetProvider.updateAllWidgets(requireContext());
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }

    // Подготавливает список приложений с состоянием
    private void prepareAppItems() {
        appItems = new ArrayList<>();
        List<String> currentPackages = category.getPackageNames();

        for (AppInfo app : allApps) {
            AppItem item = new AppItem();
            item.packageName = app.getPackageName();
            item.appName = app.getAppName();
            item.icon = app.getIcon();
            if (currentPackages.contains(item.packageName)) {
                item.state = 1;
            } else {
                item.state = 0;
            }
            appItems.add(item);
        }
    }

    // Внутренний класс для хранения данных приложения и состояния
    private static class AppItem {
        String packageName;
        String appName;
        Drawable icon;
        int state;
    }

    // Адаптер для списка приложений
    private class AppItemAdapter extends BaseAdapter {
        private Context context;
        private List<AppItem> items;
        private LayoutInflater inflater;

        AppItemAdapter(Context context, List<AppItem> items) {
            this.context = context;
            this.items = items;
            this.inflater = LayoutInflater.from(context);
        }

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
                case 1:
                    color = 0xFF4CAF50;
                    break;
                case 2:
                    color = 0xFFF44336;
                    break;
                default:
                    color = 0xFF9E9E9E;
            }
            holder.stateIndicator.setBackgroundColor(color);

            convertView.setOnClickListener(v -> {
                item.state = (item.state + 1) % 3;
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