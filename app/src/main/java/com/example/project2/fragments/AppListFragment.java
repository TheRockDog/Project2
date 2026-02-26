package com.example.project2.fragments;

import android.os.Bundle;

import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project2.R;
import com.example.project2.adapters.AppRecyclerAdapter;
import com.example.project2.dialogs.AppCategoryDialog;
import com.example.project2.models.AppInfo;
import com.example.project2.utils.AppManager;

import java.util.ArrayList;
import java.util.List;

public class AppListFragment extends BaseListFragment<AppInfo> {

    private static final String ARG_CATEGORY = "category";
    private String category;

    public static AppListFragment newInstance(String category) {
        AppListFragment fragment = new AppListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY);
        }
    }

    @Override
    protected LiveData<List<AppInfo>> getLiveData() {
        return AppManager.getAllAppsLiveData();
    }

    @Override
    protected RecyclerView.Adapter createAdapter(List<AppInfo> data) {
        return new AppRecyclerAdapter(requireContext(), data, app -> {
            AppCategoryDialog dialog = AppCategoryDialog.newInstance(app.getPackageName());
            dialog.setListener(() -> {
            });
            dialog.show(getParentFragmentManager(), "app_category");
        });
    }

    @Override
    protected List<AppInfo> filterData(List<AppInfo> data) {
        if (category == null || category.equals("All")) {
            return new ArrayList<>(data);
        }
        List<AppInfo> filtered = new ArrayList<>();
        for (AppInfo app : data) {
            if (category.equals(app.getAutoCategory())) {
                filtered.add(app);
            }
        }
        return filtered;
    }

    @Override
    protected String getEmptyText() {
        return "Нет приложений в категории " + category;
    }
}