package com.billingapp.android.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * A generated bill. Uses an auto-increment local id (rather than a UUID) since this
 * app is fully standalone/single-device - a sequential id gives naturally readable
 * invoice numbers like "INV-0001".
 */
@Entity(tableName = "bills")
public class Bill {

    @PrimaryKey(autoGenerate = true)
    public long billId;

    // Optional - a walk-in customer can leave these blank.
    public String customerName;
    public String customerPhone;

    public long createdAt = System.currentTimeMillis();

    public double totalAmount;

    // Pure local bookkeeping - no payment gateway. Lets staff mark cash/UPI received
    // without any payment UI.
    public boolean isPaid = false;

    public String displayNumber() {
        return "INV-" + String.format(java.util.Locale.getDefault(), "%04d", billId);
    }
}
