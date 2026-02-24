package com.example.project2.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project2.AppAdapter;
import com.example.project2.R;
import com.example.project2.utils.AppManager;

public class AppsFragment extends Fragment {

    private static final String ARG_CATEGORY = "category";

    public static AppsFragment newInstance(String category) {
        AppsFragment fragment = new AppsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apps, container, false);
        GridView gridView = view.findViewById(R.id.grid_view);
        String category = getArguments().getString(ARG_CATEGORY);

        AppManager.getAppsByCategoryAsync(requireContext(), category, apps -> {
            AppAdapter adapter = new AppAdapter(requireContext(), apps);
            gridView.setAdapter(adapter);
        });

        return view;
    }
}