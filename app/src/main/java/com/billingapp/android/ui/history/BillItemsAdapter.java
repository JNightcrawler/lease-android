package com.billingapp.android.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.billingapp.android.data.entity.BillItem;
import com.billingapp.android.databinding.ItemCartBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/** Read-only line item list for a past bill - reuses the cart row layout, minus the remove button. */
public class BillItemsAdapter extends RecyclerView.Adapter<BillItemsAdapter.ViewHolder> {

    private final List<BillItem> items = new ArrayList<>();

    public void setItems(List<BillItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        binding.btnRemove.setVisibility(View.GONE);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCartBinding binding;

        ViewHolder(ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(BillItem item) {
            binding.cartItemName.setText(item.productName);
            String priceFormatted = NumberFormat.getCurrencyInstance().format(item.unitPrice);
            String totalFormatted = NumberFormat.getCurrencyInstance().format(item.lineTotal);
            binding.cartItemDetail.setText(item.quantity + " " + item.unit + " x " + priceFormatted + " = " + totalFormatted);
        }
    }
}
