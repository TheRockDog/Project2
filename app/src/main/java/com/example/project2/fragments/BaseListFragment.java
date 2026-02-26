package com.example.project2.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project2.R;

import java.util.List;

public abstract class BaseListFragment<T> extends Fragment {

    protected RecyclerView recyclerView;
    protected TextView emptyView;
    protected RecyclerView.Adapter adapter;
    protected List<T> currentData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        emptyView = view.findViewById(R.id.empty_view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), getSpanCount()));

        getLiveData().observe(getViewLifecycleOwner(), new Observer<List<T>>() {
            @Override
            public void onChanged(List<T> data) {
                List<T> filtered = filterData(data);
                currentData = filtered;

                if (filtered == null || filtered.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText(getEmptyText());
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }

                if (adapter == null) {
                    adapter = createAdapter(filtered);
                    recyclerView.setAdapter(adapter);
                } else {
                    updateAdapter(filtered);
                }
            }
        });
    }

    protected int getSpanCount() {
        return 2;
    }

    protected String getEmptyText() {
        return "Нет данных";
    }

    protected List<T> filterData(List<T> data) {
        return data;
    }

    @SuppressWarnings("unchecked")
    protected void updateAdapter(List<T> filtered) {
        if (adapter instanceof UpdatableAdapter) {
            ((UpdatableAdapter<T>) adapter).updateData(filtered);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    protected abstract LiveData<List<T>> getLiveData();
    protected abstract RecyclerView.Adapter createAdapter(List<T> data);

    public interface UpdatableAdapter<T> {
        void updateData(List<T> newData);
    }
}