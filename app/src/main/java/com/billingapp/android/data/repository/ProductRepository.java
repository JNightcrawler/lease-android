package com.billingapp.android.data.repository;

import androidx.lifecycle.LiveData;

import com.billingapp.android.data.AppDatabase;
import com.billingapp.android.data.dao.ProductDao;
import com.billingapp.android.data.entity.Product;
import com.billingapp.android.util.AppExecutors;
import com.billingapp.android.util.ResultCallback;

import java.util.List;

public class ProductRepository {

    private final ProductDao dao;
    private final AppExecutors executors = AppExecutors.getInstance();

    public ProductRepository(android.content.Context context) {
        this.dao = AppDatabase.getInstance(context).productDao();
    }

    public LiveData<List<Product>> getAll() {
        return dao.getAll();
    }

    public LiveData<List<Product>> getAllInStock() {
        return dao.getAllInStock();
    }

    public void getById(String id, ResultCallback<Product> callback) {
        executors.diskIO(() -> {
            Product product = dao.getByIdSync(id);
            executors.mainThread(() -> {
                if (product != null) {
                    callback.onSuccess(product);
                } else {
                    callback.onError("Product not found");
                }
            });
        });
    }

    public void create(Product product, ResultCallback<Product> callback) {
        executors.diskIO(() -> {
            try {
                dao.insert(product);
                executors.mainThread(() -> callback.onSuccess(product));
            } catch (Exception e) {
                executors.mainThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save product"));
            }
        });
    }

    public void update(Product product, ResultCallback<Product> callback) {
        executors.diskIO(() -> {
            try {
                product.updatedAt = System.currentTimeMillis();
                dao.update(product);
                executors.mainThread(() -> callback.onSuccess(product));
            } catch (Exception e) {
                executors.mainThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to update product"));
            }
        });
    }

    public void delete(Product product, ResultCallback<Void> callback) {
        executors.diskIO(() -> {
            try {
                dao.delete(product);
                executors.mainThread(() -> callback.onSuccess(null));
            } catch (Exception e) {
                executors.mainThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to delete product"));
            }
        });
    }
}
