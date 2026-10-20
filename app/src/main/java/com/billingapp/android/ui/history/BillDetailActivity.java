package com.billingapp.android.ui.history;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.billingapp.android.data.entity.Bill;
import com.billingapp.android.data.entity.BillItem;
import com.billingapp.android.data.repository.BillRepository;
import com.billingapp.android.databinding.ActivityBillDetailBinding;
import com.billingapp.android.print.PrintCallback;
import com.billingapp.android.print.PrinterServiceFactory;
import com.billingapp.android.util.ResultCallback;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BillDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BILL_ID = "billId";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    private ActivityBillDetailBinding binding;
    private BillRepository repository;
    private BillItemsAdapter itemsAdapter;

    private Bill currentBill;
    private List<BillItem> currentItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long billId = getIntent().getLongExtra(EXTRA_BILL_ID, -1);
        if (billId == -1) {
            finish();
            return;
        }

        repository = new BillRepository(getApplicationContext());

        itemsAdapter = new BillItemsAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(itemsAdapter);

        repository.getById(billId).observe(this, bill -> {
            if (bill == null) return;
            currentBill = bill;
            renderBill(bill);
        });

        repository.getItemsForBill(billId).observe(this, items -> {
            currentItems = items;
            itemsAdapter.setItems(items);
        });

        binding.btnTogglePaid.setOnClickListener(v -> togglePaid());
        binding.btnReprint.setOnClickListener(v -> reprint());
    }

    private void renderBill(Bill bill) {
        binding.billNumber.setText(bill.displayNumber());
        binding.billDate.setText(DATE_FORMAT.format(new java.util.Date(bill.createdAt)));

        StringBuilder customer = new StringBuilder();
        if (bill.customerName != null) customer.append(bill.customerName);
        if (bill.customerPhone != null) {
            if (customer.length() > 0) customer.append(" - ");
            customer.append(bill.customerPhone);
        }
        binding.billCustomer.setText(customer.toString());
        binding.billCustomer.setVisibility(customer.length() > 0 ? View.VISIBLE : View.GONE);

        binding.billTotal.setText("Total: " + NumberFormat.getCurrencyInstance().format(bill.totalAmount));
        binding.btnTogglePaid.setText(bill.isPaid ? "Mark as unpaid" : "Mark as paid");
    }

    private void togglePaid() {
        if (currentBill == null) return;
        repository.markPaid(currentBill, !currentBill.isPaid, new ResultCallback<Bill>() {
            @Override
            public void onSuccess(Bill result) {
                // LiveData observer above updates the UI automatically.
            }

            @Override
            public void onError(String message) {
                showStatus(message);
            }
        });
    }

    private void reprint() {
        if (currentBill == null || currentItems == null) return;

        binding.btnReprint.setEnabled(false);
        PrinterServiceFactory.get(this).printBill(this, currentBill, currentItems, new PrintCallback() {
            @Override
            public void onSuccess() {
                binding.btnReprint.setEnabled(true);
                showStatus("Sent to printer");
            }

            @Override
            public void onError(String message) {
                binding.btnReprint.setEnabled(true);
                showStatus("Print failed: " + message);
            }
        });
    }

    private void showStatus(String message) {
        binding.statusText.setText(message);
        binding.statusText.setVisibility(View.VISIBLE);
    }
}
