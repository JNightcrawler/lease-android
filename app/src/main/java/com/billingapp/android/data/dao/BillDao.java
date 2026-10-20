package com.billingapp.android.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.billingapp.android.data.entity.Bill;
import com.billingapp.android.data.entity.BillItem;

import java.util.List;

@Dao
public interface BillDao {

    @Insert
    long insertBill(Bill bill);

    @Insert
    void insertItems(List<BillItem> items);

    @Update
    void updateBill(Bill bill);

    @Query("SELECT * FROM bills ORDER BY createdAt DESC")
    LiveData<List<Bill>> getAll();

    @Query("SELECT * FROM bills WHERE billId = :billId LIMIT 1")
    LiveData<Bill> getById(long billId);

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    LiveData<List<BillItem>> getItemsForBill(long billId);
}
