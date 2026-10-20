package com.billingapp.android.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.billingapp.android.data.AppDatabase;
import com.billingapp.android.data.CartItem;
import com.billingapp.android.data.dao.BillDao;
import com.billingapp.android.data.dao.ProductDao;
import com.billingapp.android.data.entity.Bill;
import com.billingapp.android.data.entity.BillItem;
import com.billingapp.android.data.entity.Product;
import com.billingapp.android.util.AppExecutors;
import com.billingapp.android.util.ResultCallback;

import java.util.ArrayList;
import java.util.List;

public class BillRepository {

    private final AppDatabase db;
    private final BillDao billDao;
    private final ProductDao productDao;
    private final AppExecutors executors = AppExecutors.getInstance();

    public BillRepository(Context context) {
        this.db = AppDatabase.getInstance(context);
        this.billDao = db.billDao();
        this.productDao = db.productDao();
    }

    public LiveData<List<Bill>> getAll() {
        return billDao.getAll();
    }

    public LiveData<Bill> getById(long billId) {
        return billDao.getById(billId);
    }

    public LiveData<List<BillItem>> getItemsForBill(long billId) {
        return billDao.getItemsForBill(billId);
    }

    /**
     * Creates a bill from the given cart: re-validates stock against the current
     * database state (not just what the cart screen last showed), decrements stock,
     * and inserts the Bill + its BillItems - all inside one Room transaction, so a
     * mid-way stock failure rolls back everything rather than leaving a half-billed,
     * half-decremented state.
     */
    public void createBill(List<CartItem> cartItems, String customerName, String customerPhone, ResultCallback<Bill> callback) {
        if (cartItems == null || cartItems.isEmpty()) {
            callback.onError("Add at least one item before generating a bill");
            return;
        }

        executors.diskIO(() -> {
            try {
                Bill bill = new Bill();
                bill.customerName = (customerName == null || customerName.isBlank()) ? null : customerName.trim();
                bill.customerPhone = (customerPhone == null || customerPhone.isBlank()) ? null : customerPhone.trim();

                double[] totalHolder = {0};

                db.runInTransaction(() -> {
                    List<BillItem> items = new ArrayList<>();
                    long now = System.currentTimeMillis();

                    for (CartItem cartItem : cartItems) {
                        Product product = productDao.getForBilling(cartItem.productId);
                        if (product == null) {
                            throw new IllegalStateException("\"" + cartItem.productName + "\" no longer exists");
                        }
                        if (product.quantity < cartItem.quantity) {
                            throw new IllegalStateException(
                                    "Insufficient stock for \"" + product.name + "\". Available: "
                                            + product.quantity + ", Requested: " + cartItem.quantity);
                        }

                        BillItem item = new BillItem();
                        item.productId = product.id;
                        item.productName = product.name;
                        item.unit = product.unit;
                        item.unitPrice = product.price;
                        item.quantity = cartItem.quantity;
                        item.lineTotal = product.price * cartItem.quantity;
                        items.add(item);

                        totalHolder[0] += item.lineTotal;

                        productDao.decrementStock(product.id, cartItem.quantity, now);
                    }

                    bill.totalAmount = totalHolder[0];
                    long billId = billDao.insertBill(bill);
                    bill.billId = billId;

                    for (BillItem item : items) {
                        item.billId = billId;
                    }
                    billDao.insertItems(items);
                });

                executors.mainThread(() -> callback.onSuccess(bill));
            } catch (Exception e) {
                String message = e.getMessage() != null ? e.getMessage() : "Failed to generate bill";
                executors.mainThread(() -> callback.onError(message));
            }
        });
    }

    public void markPaid(Bill bill, boolean paid, ResultCallback<Bill> callback) {
        executors.diskIO(() -> {
            try {
                bill.isPaid = paid;
                billDao.updateBill(bill);
                executors.mainThread(() -> callback.onSuccess(bill));
            } catch (Exception e) {
                executors.mainThread(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to update bill"));
            }
        });
    }
}
