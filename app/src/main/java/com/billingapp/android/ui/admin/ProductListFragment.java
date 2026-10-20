package com.billingapp.android.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.billingapp.android.data.entity.Product;
import com.billingapp.android.data.repository.ProductRepository;
import com.billingapp.android.databinding.FragmentProductListBinding;
import com.billingapp.android.util.ResultCallback;

import java.util.List;

public class ProductListFragment extends Fragment {

    private FragmentProductListBinding binding;
    private ProductRepository repository;
    private ProductAdapter adapter;

    public static ProductListFragment newInstance() {
        return new ProductListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProductListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ProductRepository(requireContext());

        adapter = new ProductAdapter(new ProductAdapter.Listener() {
            @Override
            public void onEdit(Product product) {
                Intent intent = new Intent(requireContext(), ProductFormActivity.class);
                intent.putExtra(ProductFormActivity.EXTRA_PRODUCT_ID, product.id);
                startActivity(intent);
            }

            @Override
            public void onDelete(Product product) {
                confirmDelete(product);
            }
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);

        binding.fabAdd.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProductFormActivity.class)));

        repository.getAll().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<Product> products) {
        adapter.submitList(products);
        binding.emptyText.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete(Product product) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete product?")
                .setMessage("Delete \"" + product.name + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) ->
                        repository.delete(product, new ResultCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                // LiveData observer above updates the list automatically.
                            }

                            @Override
                            public void onError(String message) {
                                new MaterialAlertDialogBuilder(requireContext())
                                        .setTitle("Couldn't delete")
                                        .setMessage(message)
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
