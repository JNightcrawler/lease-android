package com.billingapp.android.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.UUID;

/** A material or utility item managed in the Admin section and sold via Billing. */
@Entity(tableName = "products")
public class Product {

    @NonNull
    @PrimaryKey
    public String id = UUID.randomUUID().toString();

    public String name;

    public double price;

    // Nullable - epoch millis. Null means "does not expire" (e.g. non-perishable materials).
    public Long expiryDate;

    public int quantity;

    // Unit of measurement - "pcs", "kg", "ltr", etc. Defaults to "pcs" since not every
    // material/utility needs a custom unit.
    public String unit = "pcs";

    // Optional grouping, e.g. "Material" vs "Utility". Nullable - not required.
    public String category;

    public long createdAt = System.currentTimeMillis();
    public long updatedAt = System.currentTimeMillis();
}
