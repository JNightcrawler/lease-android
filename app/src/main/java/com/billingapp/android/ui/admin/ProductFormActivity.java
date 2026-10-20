package com.billingapp.android.ui.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.billingapp.android.data.entity.Product;
import com.billingapp.android.data.repository.ProductRepository;
import com.billingapp.android.databinding.ActivityProductFormBinding;
import com.billingapp.android.util.ResultCallback;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Handles both create (no EXTRA_PRODUCT_ID) and edit (EXTRA_PRODUCT_ID provided) flows. */
public class ProductFormActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "productId";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private ActivityProductFormBinding binding;
    private ProductRepository repository;
    private String productId;
    private Product existingProduct;
    private Long selectedExpiryDate; // epoch millis, nullable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new ProductRepository(getApplicationContext());
        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        boolean isEdit = productId != null;

        binding.toolbar.setTitle(isEdit ? "Edit Product" : "Add Product");
        binding.inputUnit.setText("pcs");

        if (isEdit) {
            repository.getById(productId, new ResultCallback<Product>() {
                @Override
                public void onSuccess(Product product) {
                    existingProduct = product;
                    populateForm(product);
                }

                @Override
                public void onError(String message) {
                    showError(message);
                }
            });
        }

        binding.btnPickDate.setOnClickListener(v -> showDatePicker());
        binding.btnClearDate.setOnClickListener(v -> {
            selectedExpiryDate = null;
            binding.expiryDateText.setText("Not set - does not expire");
            binding.btnClearDate.setVisibility(View.GONE);
        });

        binding.btnSave.setOnClickListener(v -> onSave(isEdit));
    }

    private void populateForm(Product product) {
        binding.inputName.setText(product.name);
        binding.inputPrice.setText(String.valueOf(product.price));
        binding.inputQuantity.setText(String.valueOf(product.quantity));
        binding.inputUnit.setText(product.unit);
        binding.inputCategory.setText(product.category);

        if (product.expiryDate != null) {
            selectedExpiryDate = product.expiryDate;
            binding.expiryDateText.setText(DATE_FORMAT.format(new Date(product.expiryDate)));
            binding.btnClearDate.setVisibility(View.VISIBLE);
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedExpiryDate != null) {
            calendar.setTimeInMillis(selectedExpiryDate);
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, dayOfMonth, 0, 0, 0);
            picked.set(Calendar.MILLISECOND, 0);
            selectedExpiryDate = picked.getTimeInMillis();
            binding.expiryDateText.setText(DATE_FORMAT.format(picked.getTime()));
            binding.btnClearDate.setVisibility(View.VISIBLE);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void onSave(boolean isEdit) {
        String name = text(binding.inputName);
        String priceStr = text(binding.inputPrice);
        String quantityStr = text(binding.inputQuantity);
        String unit = text(binding.inputUnit);
        String category = text(binding.inputCategory);

        if (name.isEmpty() || priceStr.isEmpty() || quantityStr.isEmpty()) {
            showError("Name, price, and quantity are required");
            return;
        }

        double price;
        int quantity;
        try {
            price = Double.parseDouble(priceStr);
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            showError("Price and quantity must be valid numbers");
            return;
        }

        if (price < 0 || quantity < 0) {
            showError("Price and quantity cannot be negative");
            return;
        }

        hideError();

        Product product = isEdit ? existingProduct : new Product();
        product.name = name;
        product.price = price;
        product.quantity = quantity;
        product.unit = unit.isEmpty() ? "pcs" : unit;
        product.category = category.isEmpty() ? null : category;
        product.expiryDate = selectedExpiryDate;

        ResultCallback<Product> callback = new ResultCallback<Product>() {
            @Override
            public void onSuccess(Product result) {
                finish();
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        };

        if (isEdit) {
            repository.update(product, callback);
        } else {
            repository.create(product, callback);
        }
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
}
