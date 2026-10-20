package com.billingapp.android.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.billingapp.android.data.dao.BillDao;
import com.billingapp.android.data.dao.ProductDao;
import com.billingapp.android.data.entity.Bill;
import com.billingapp.android.data.entity.BillItem;
import com.billingapp.android.data.entity.Product;

@Database(entities = {Product.class, Bill.class, BillItem.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract ProductDao productDao();

    public abstract BillDao billDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "billing_app.db")
                            .build();
                }
            }
        }
        return instance;
    }
}
