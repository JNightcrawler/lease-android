package com.billingapp.android.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One line item on a bill. Snapshots the product name and price at time of sale
 * (rather than joining live to Product), so a bill stays accurate and readable
 * even if a product is later renamed, repriced, or deleted.
 */
@Entity(
        tableName = "bill_items",
        foreignKeys = @ForeignKey(
                entity = Bill.class,
                parentColumns = "billId",
                childColumns = "billId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("billId"), @Index("productId")}
)
public class BillItem {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long billId;

    // Not a Room foreign key on purpose - the product may be deleted later and this
    // line item should still display fine using the snapshot fields below.
    public String productId;

    public String productName;
    public String unit;
    public double unitPrice;
    public int quantity;
    public double lineTotal;
}
