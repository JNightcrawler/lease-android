package com.billingapp.android.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.billingapp.android.R;
import com.billingapp.android.data.entity.Product;
import com.billingapp.android.databinding.ItemProductBinding;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    // Defaults - easy to make configurable later if needed.
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final long EXPIRING_SOON_WINDOW_MS = TimeUnit.DAYS.toMillis(7);

    public interface Listener {
        void onEdit(Product product);
        void onDelete(Product product);
    }

    private final List<Product> items = new ArrayList<>();
    private final Listener listener;

    public ProductAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<Product> newItems) {
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
                return items.get(oldPos).id.equals(newItems.get(newPos).id);
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                Product a = items.get(oldPos);
                Product b = newItems.get(newPos);
                return a.name.equals(b.name) && a.price == b.price && a.quantity == b.quantity
                        && java.util.Objects.equals(a.expiryDate, b.expiryDate);
            }
        });
        items.clear();
        items.addAll(newItems);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding binding = ItemProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        private final ItemProductBinding binding;
        private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        ViewHolder(ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Product product, Listener listener) {
            binding.productName.setText(product.name);
            binding.productPrice.setText(NumberFormat.getCurrencyInstance().format(product.price) + " / " + product.unit);
            binding.productStock.setText("Stock: " + product.quantity + " " + product.unit);

            boolean lowStock = product.quantity > 0 && product.quantity <= LOW_STOCK_THRESHOLD;
            binding.badgeLowStock.setVisibility(lowStock ? View.VISIBLE : View.GONE);

            if (product.expiryDate != null) {
                long now = System.currentTimeMillis();
                if (product.expiryDate < now) {
                    binding.badgeExpiry.setText("Expired");
                    binding.badgeExpiry.setBackgroundColor(binding.getRoot().getContext().getResources().getColor(R.color.errorRed, null));
                    binding.badgeExpiry.setVisibility(View.VISIBLE);
                } else if (product.expiryDate - now <= EXPIRING_SOON_WINDOW_MS) {
                    binding.badgeExpiry.setText("Expires " + DATE_FORMAT.format(new java.util.Date(product.expiryDate)));
                    binding.badgeExpiry.setBackgroundColor(binding.getRoot().getContext().getResources().getColor(R.color.warningAmber, null));
                    binding.badgeExpiry.setVisibility(View.VISIBLE);
                } else {
                    binding.badgeExpiry.setVisibility(View.GONE);
                }
            } else {
                binding.badgeExpiry.setVisibility(View.GONE);
            }

            binding.btnEdit.setOnClickListener(v -> listener.onEdit(product));
            binding.btnDelete.setOnClickListener(v -> listener.onDelete(product));
        }
    }
}
