package com.billingapp.android.ui.billing;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.billingapp.android.data.CartItem;
import com.billingapp.android.data.entity.Bill;
import com.billingapp.android.data.entity.BillItem;
import com.billingapp.android.data.entity.Product;
import com.billingapp.android.data.repository.BillRepository;
import com.billingapp.android.data.repository.ProductRepository;
import com.billingapp.android.databinding.DialogPickProductBinding;
import com.billingapp.android.databinding.FragmentBillingHomeBinding;
import com.billingapp.android.print.PrintCallback;
import com.billingapp.android.print.PrinterServiceFactory;
import com.billingapp.android.ui.settings.PrinterSetupActivity;
import com.billingapp.android.util.ResultCallback;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class BillingHomeFragment extends Fragment {

    private FragmentBillingHomeBinding binding;
    private ProductRepository productRepository;
    private BillRepository billRepository;
    private CartAdapter cartAdapter;

    private final List<CartItem> cart = new ArrayList<>();
    private List<Product> availableProducts = new ArrayList<>();

    public static BillingHomeFragment newInstance() {
        return new BillingHomeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBillingHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        productRepository = new ProductRepository(requireContext());
        billRepository = new BillRepository(requireContext());

        cartAdapter = new CartAdapter(item -> {
            cart.remove(item);
            renderCart();
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(cartAdapter);

        binding.toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
        binding.btnAddItem.setOnClickListener(v -> showAddItemDialog());
        binding.btnGenerateAndPrint.setOnClickListener(v -> generateAndPrint());

        productRepository.getAllInStock().observe(getViewLifecycleOwner(), products -> availableProducts = products);

        renderCart();
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == com.billingapp.android.R.id.action_printer_setup) {
            startActivity(new Intent(requireContext(), PrinterSetupActivity.class));
            return true;
        }
        return false;
    }

    private void showAddItemDialog() {
        if (availableProducts.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("No products in stock")
                    .setMessage("Add products with available stock from the Admin tab first.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        DialogPickProductBinding dialogBinding = DialogPickProductBinding.inflate(LayoutInflater.from(requireContext()));

        List<String> names = new ArrayList<>();
        for (Product p : availableProducts) {
            names.add(p.name + " (" + p.quantity + " " + p.unit + " available)");
        }
        AutoCompleteTextView dropdown = dialogBinding.dropdownProduct;
        dropdown.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, names));

        final Product[] selected = new Product[1];
        dropdown.setOnItemClickListener((parent, v, position, id) -> selected[0] = availableProducts.get(position));

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add item")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Product product = selected[0];
            String qtyStr = dialogBinding.inputQuantity.getText() != null
                    ? dialogBinding.inputQuantity.getText().toString().trim() : "";

            if (product == null) {
                dialogBinding.dialogErrorText.setText("Select a product");
                dialogBinding.dialogErrorText.setVisibility(View.VISIBLE);
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(qtyStr);
            } catch (NumberFormatException e) {
                dialogBinding.dialogErrorText.setText("Enter a valid quantity");
                dialogBinding.dialogErrorText.setVisibility(View.VISIBLE);
                return;
            }

            int alreadyInCart = cartQuantityFor(product.id);
            if (qty < 1 || qty + alreadyInCart > product.quantity) {
                dialogBinding.dialogErrorText.setText("Only " + (product.quantity - alreadyInCart) + " more available");
                dialogBinding.dialogErrorText.setVisibility(View.VISIBLE);
                return;
            }

            addToCart(product, qty);
            dialog.dismiss();
        }));

        dialog.show();
    }

    private int cartQuantityFor(String productId) {
        for (CartItem item : cart) {
            if (item.productId.equals(productId)) return item.quantity;
        }
        return 0;
    }

    private void addToCart(Product product, int quantity) {
        for (CartItem item : cart) {
            if (item.productId.equals(product.id)) {
                item.quantity += quantity;
                renderCart();
                return;
            }
        }
        cart.add(new CartItem(product.id, product.name, product.unit, product.price, product.quantity, quantity));
        renderCart();
    }

    private void renderCart() {
        cartAdapter.setItems(cart);
        binding.emptyCartText.setVisibility(cart.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(cart.isEmpty() ? View.GONE : View.VISIBLE);

        double total = 0;
        for (CartItem item : cart) total += item.lineTotal();
        binding.totalText.setText("Total: " + NumberFormat.getCurrencyInstance().format(total));

        binding.btnGenerateAndPrint.setEnabled(!cart.isEmpty());
    }

    private void generateAndPrint() {
        hideError();
        String customerName = text(binding.inputCustomerName);
        String customerPhone = text(binding.inputCustomerPhone);

        binding.btnGenerateAndPrint.setEnabled(false);

        billRepository.createBill(cart, customerName, customerPhone, new ResultCallback<Bill>() {
            @Override
            public void onSuccess(Bill bill) {
                // Self-unsubscribing observer: Room's LiveData invalidates on any write to the
                // bill_items table, not just writes affecting this bill, so without removing the
                // observer after the first real (non-empty) emission, a later unrelated bill could
                // accidentally re-trigger printing of this one.
                LiveData<List<BillItem>> itemsLiveData = billRepository.getItemsForBill(bill.billId);
                Observer<List<BillItem>>[] observerHolder = new Observer[1];
                observerHolder[0] = items -> {
                    if (items == null || items.isEmpty()) return; // initial emission before the insert lands

                    itemsLiveData.removeObserver(observerHolder[0]);

                    PrinterServiceFactory.get(requireContext()).printBill(requireContext(), bill, items, new PrintCallback() {
                        @Override
                        public void onSuccess() {
                            resetForm();
                        }

                        @Override
                        public void onError(String message) {
                            resetForm();
                            showError("Bill saved, but printing failed: " + message);
                        }
                    });
                };
                itemsLiveData.observe(getViewLifecycleOwner(), observerHolder[0]);
            }

            @Override
            public void onError(String message) {
                binding.btnGenerateAndPrint.setEnabled(true);
                showError(message);
            }
        });
    }

    private void resetForm() {
        cart.clear();
        renderCart();
        binding.inputCustomerName.setText("");
        binding.inputCustomerPhone.setText("");
    }

    private String text(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void showError(String message) {
        binding.errorText.setText(message);
        binding.errorText.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        binding.errorText.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
