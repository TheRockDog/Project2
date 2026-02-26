package com.example.project2.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.project2.R;
import com.example.project2.models.AppInfo;
import com.example.project2.models.Category;
import com.example.project2.utils.AppManager;
import com.example.project2.utils.CategoryManager;

import java.util.ArrayList;
import java.util.List;

public class AppCategoryDialog extends DialogFragment {

    private static final String ARG_PACKAGE = "package";
    private static final String ARG_STATES = "states";
    private static final String ARG_ORIGINAL = "original";

    private String packageName;
    private AppInfo app;
    private CategoryManager categoryManager;
    private List<Category> userCategories;
    private int[] states;          // 0-нет, 1-выбрана, 2-будет удалена
    private boolean[] original;     // исходное состояние
    private List<Object> items;
    private ListView listView;
    private DialogInterfaceListener listener;

    public interface DialogInterfaceListener {
        void onCategoriesChanged();
    }

    public static AppCategoryDialog newInstance(String packageName) {
        AppCategoryDialog dialog = new AppCategoryDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE, packageName);
        dialog.setArguments(args);
        return dialog;
    }

    public static AppCategoryDialog newInstance(String packageName, int[] savedStates, boolean[] savedOriginal) {
        AppCategoryDialog dialog = new AppCategoryDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE, packageName);
        args.putIntArray(ARG_STATES, savedStates);
        args.putBooleanArray(ARG_ORIGINAL, savedOriginal);
        dialog.setArguments(args);
        return dialog;
    }

    public void setListener(DialogInterfaceListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            packageName = getArguments().getString(ARG_PACKAGE);
            app = AppManager.getAppByPackageName(packageName);
            if (app == null) {
                dismiss();
                return;
            }
            categoryManager = CategoryManager.getInstance(requireContext());

            List<Category> all = categoryManager.getAllCategories();
            userCategories = new ArrayList<>();
            for (Category cat : all) {
                if (!cat.isBuiltIn()) {
                    userCategories.add(cat);
                }
            }

            List<Integer> appCategoryIds = categoryManager.getAppCategories(packageName);
            int size = userCategories.size();
            if (savedInstanceState == null) {
                int[] savedStates = getArguments().getIntArray(ARG_STATES);
                boolean[] savedOriginal = getArguments().getBooleanArray(ARG_ORIGINAL);
                if (savedStates != null && savedOriginal != null) {
                    states = savedStates;
                    original = savedOriginal;
                } else {
                    states = new int[size];
                    original = new boolean[size];
                    for (int i = 0; i < size; i++) {
                        boolean isIn = appCategoryIds.contains(userCategories.get(i).getId());
                        original[i] = isIn;
                        states[i] = isIn ? 1 : 0;
                    }
                }
            } else {
                states = savedInstanceState.getIntArray("states");
                original = savedInstanceState.getBooleanArray("original");
            }
        }
        items = new ArrayList<>(userCategories);
        items.add("CREATE");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray("states", states);
        outState.putBooleanArray("original", original);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_category_select, null);
        listView = view.findViewById(R.id.list_categories);
        listView.setAdapter(new CategorySelectAdapter());

        builder.setTitle("Категории для " + app.getAppName())
                .setView(view)
                .setPositiveButton("Готово", (dialog, which) -> saveChanges())
                .setNegativeButton("Отмена", (dialog, which) -> dismiss());

        AlertDialog dialog = builder.create();

        listView.setOnItemClickListener((parent, v, position, id) -> {
            if (position < userCategories.size()) {
                if (original[position]) {
                    states[position] = (states[position] == 1) ? 2 : 1;
                } else {
                    states[position] = (states[position] == 0) ? 1 : 0;
                }
                View indicator = v.findViewById(R.id.state_indicator);
                updateIndicator(indicator, states[position]);
            } else {
                // Сохранение текущего состояния
                int[] currentStates = states.clone();
                boolean[] currentOriginal = original.clone();

                // Закрытие текущего диалога
                dialog.dismiss();

                // Получение FragmentManager активности
                androidx.fragment.app.FragmentManager fm = requireActivity().getSupportFragmentManager();

                // Открытие диалога ввода имени
                CategoryNameDialog.newInstance(new CategoryNameDialog.Listener() {
                    @Override
                    public void onNameEntered(String name) {
                        try {
                            Category newCategory = categoryManager.createCategory(name);
                            if (newCategory != null) {
                                categoryManager.addAppToCategory(packageName, newCategory.getId());
                                if (listener != null) listener.onCategoriesChanged();
                            }
                            // Небольшая задержка для завершения закрытия предыдущего диалога
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    AppCategoryDialog newDialog = AppCategoryDialog.newInstance(packageName, currentStates, currentOriginal);
                                    newDialog.setListener(listener);
                                    newDialog.show(fm, "app_category");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(requireContext(), "Ошибка открытия диалога", Toast.LENGTH_SHORT).show();
                                }
                            }, 150);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(requireContext(), "Ошибка создания категории", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancel() {
                        try {
                            AppCategoryDialog newDialog = AppCategoryDialog.newInstance(packageName, currentStates, currentOriginal);
                            newDialog.setListener(listener);
                            newDialog.show(fm, "app_category");
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(requireContext(), "Ошибка открытия диалога", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).show(fm, "name_dialog");
            }
        });

        return dialog;
    }

    private void saveChanges() {
        boolean changed = false;
        int added = 0, removed = 0;
        for (int i = 0; i < userCategories.size(); i++) {
            int newState = states[i];
            boolean wasChecked = original[i];
            Category cat = userCategories.get(i);
            if (newState == 1 && !wasChecked) {
                categoryManager.addAppToCategory(packageName, cat.getId());
                added++;
                changed = true;
            } else if ((newState == 0 || newState == 2) && wasChecked) {
                categoryManager.removeAppFromCategory(packageName, cat.getId());
                removed++;
                changed = true;
            }
        }
        if (changed) {
            String message;
            if (added > 0 && removed > 0) {
                message = "Изменения сохранены";
            } else if (added > 0) {
                message = "Приложение добавлено в категории";
            } else {
                message = "Приложение удалено из категорий";
            }
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
        if (changed && listener != null) {
            listener.onCategoriesChanged();
        }
        dismiss();
    }

    private void updateIndicator(View indicator, int state) {
        int color;
        switch (state) {
            case 1: color = 0xFF4CAF50; break;
            case 2: color = 0xFFF44336; break;
            default: color = 0xFF9E9E9E;
        }
        indicator.setBackgroundColor(color);
    }

    private class CategorySelectAdapter extends BaseAdapter {
        @Override
        public int getCount() { return items.size(); }

        @Override
        public Object getItem(int position) { return items.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_category_select, parent, false);
            }
            View indicator = convertView.findViewById(R.id.state_indicator);
            TextView nameView = convertView.findViewById(R.id.category_name);

            if (position < userCategories.size()) {
                Category cat = userCategories.get(position);
                nameView.setText(cat.getName());
                indicator.setVisibility(View.VISIBLE);
                updateIndicator(indicator, states[position]);
            } else {
                nameView.setText("Создать новую категорию");
                indicator.setVisibility(View.VISIBLE);
                indicator.setBackgroundColor(0xFF9E9E9E);
            }
            return convertView;
        }
    }
}