package com.billingapp.android.ui.billing;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.billingapp.android.data.CartItem;
import com.billingapp.android.databinding.ItemCartBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    public interface Listener {
        void onRemove(CartItem item);
    }

    private final List<CartItem> items = new ArrayList<>();
    private final Listener listener;

    public CartAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<CartItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        private final ItemCartBinding binding;

        ViewHolder(ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CartItem item, Listener listener) {
            binding.cartItemName.setText(item.productName);
            String priceFormatted = NumberFormat.getCurrencyInstance().format(item.unitPrice);
            String totalFormatted = NumberFormat.getCurrencyInstance().format(item.lineTotal());
            binding.cartItemDetail.setText(item.quantity + " " + item.unit + " x " + priceFormatted + " = " + totalFormatted);
            binding.btnRemove.setOnClickListener(v -> listener.onRemove(item));
        }
    }
}
