package com.billingapp.android.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.billingapp.android.data.repository.BillRepository;
import com.billingapp.android.databinding.FragmentBillHistoryBinding;

public class BillHistoryFragment extends Fragment {

    private FragmentBillHistoryBinding binding;
    private BillRepository repository;
    private BillHistoryAdapter adapter;

    public static BillHistoryFragment newInstance() {
        return new BillHistoryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBillHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new BillRepository(requireContext());

        adapter = new BillHistoryAdapter(bill -> {
            Intent intent = new Intent(requireContext(), BillDetailActivity.class);
            intent.putExtra(BillDetailActivity.EXTRA_BILL_ID, bill.billId);
            startActivity(intent);
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        repository.getAll().observe(getViewLifecycleOwner(), bills -> {
            adapter.submitList(bills);
            binding.emptyText.setVisibility(bills.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
