package com.example.project2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.example.project2.models.AppInfo;
import com.example.project2.utils.AppManager;

import java.util.ArrayList;
import java.util.List;

public class AppListFragment extends Fragment {

    private static final String ARG_CATEGORY = "category";
    private String category;
    private GridView gridView;
    private AppAdapter appAdapter;

    public static AppListFragment newInstance(String category) {
        AppListFragment fragment = new AppListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_list, container, false);
        gridView = view.findViewById(R.id.grid_view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Подписка на LiveData со списком всех приложений
        AppManager.getAllAppsLiveData().observe(getViewLifecycleOwner(), new Observer<List<AppInfo>>() {
            @Override
            public void onChanged(List<AppInfo> allApps) {
                List<AppInfo> filtered = filterAppsByCategory(allApps);
                if (appAdapter == null) {
                    appAdapter = new AppAdapter(requireContext(), filtered);
                    gridView.setAdapter(appAdapter);
                } else {
                    appAdapter.updateApps(filtered);
                }
            }
        });
    }

    // Фильтрация списка по категории
    private List<AppInfo> filterAppsByCategory(List<AppInfo> allApps) {
        if (category == null || category.equals("All")) {
            return new ArrayList<>(allApps);
        }
        List<AppInfo> filtered = new ArrayList<>();
        for (AppInfo app : allApps) {
            if (category.equals(app.getAutoCategory())) {
                filtered.add(app);
            }
        }
        return filtered;
    }
}