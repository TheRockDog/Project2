package com.example.project2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project2.models.AppInfo;
import com.example.project2.utils.AppManager;

import java.util.List;

public class AppListFragment extends Fragment {

    private static final String ARG_CATEGORY = "category";
    private String category;               // Категория для отображения
    private GridView gridView;              // Сетка приложений
    private AppAdapter appAdapter;          // Адаптер приложений

    // Создание экземпляра с категорией
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
        loadApps();
        return view;
    }

    // Загрузка приложений из AppManager
    private void loadApps() {
        AppManager.getAppsByCategoryAsync(requireContext(), category, apps -> {
            if (appAdapter == null) {
                appAdapter = new AppAdapter(requireContext(), apps);
                gridView.setAdapter(appAdapter);
            } else {
                appAdapter.updateApps(apps);
            }
            // Асинхронная загрузка иконок
            if (AppManager.hasCachedApps()) {
                AppManager.loadIconsFromFilesAsync(requireContext(), () ->
                        appAdapter.notifyDataSetChanged()
                );
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadApps(); // Обновление при возврате
    }
}