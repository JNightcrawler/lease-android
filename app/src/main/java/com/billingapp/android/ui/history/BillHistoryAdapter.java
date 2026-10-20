package com.billingapp.android.ui.history;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.billingapp.android.R;
import com.billingapp.android.data.entity.Bill;
import com.billingapp.android.databinding.ItemBillBinding;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BillHistoryAdapter extends RecyclerView.Adapter<BillHistoryAdapter.ViewHolder> {

    public interface Listener {
        void onClick(Bill bill);
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    private final List<Bill> items = new ArrayList<>();
    private final Listener listener;

    public BillHistoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Bill> newItems) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return newItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).billId == newItems.get(newPos).billId;
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                Bill a = items.get(oldPos);
                Bill b = newItems.get(newPos);
                return a.isPaid == b.isPaid && a.totalAmount == b.totalAmount;
            }
        });
        items.clear();
        items.addAll(newItems);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBillBinding binding = ItemBillBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemBillBinding binding;

        ViewHolder(ItemBillBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Bill bill, Listener listener) {
            binding.billNumber.setText(bill.displayNumber());
            binding.billDate.setText(DATE_FORMAT.format(new Date(bill.createdAt)));
            binding.billTotal.setText(NumberFormat.getCurrencyInstance().format(bill.totalAmount));

            binding.billPaidStatus.setText(bill.isPaid ? "Paid" : "Unpaid");
            int color = bill.isPaid ? R.color.successGreen : R.color.warningAmber;
            binding.billPaidStatus.setTextColor(binding.getRoot().getContext().getResources().getColor(color, null));

            binding.getRoot().setOnClickListener(v -> listener.onClick(bill));
        }
    }
}
