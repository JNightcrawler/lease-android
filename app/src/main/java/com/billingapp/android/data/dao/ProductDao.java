package com.billingapp.android.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.billingapp.android.data.entity.Product;

import java.util.List;

@Dao
public interface ProductDao {

    @Insert
    void insert(Product product);

    @Update
    void update(Product product);

    @Delete
    void delete(Product product);

    @Query("SELECT * FROM products ORDER BY name ASC")
    LiveData<List<Product>> getAll();

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    Product getByIdSync(String id);

    @Query("SELECT * FROM products WHERE quantity > 0 ORDER BY name ASC")
    LiveData<List<Product>> getAllInStock();

    // Used when reserving stock at billing time - runs on a background thread already,
    // so a synchronous read-then-write is safe within the single-writer Room setup here.
    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    Product getForBilling(String id);

    @Query("UPDATE products SET quantity = quantity - :amount, updatedAt = :now WHERE id = :id")
    void decrementStock(String id, int amount, long now);
}
