package com.billingapp.android.data;

/** In-memory cart line while a bill is being built - not persisted directly. */
public class CartItem {
    public final String productId;
    public final String productName;
    public final String unit;
    public final double unitPrice;
    public final int availableStock;
    public int quantity;

    public CartItem(String productId, String productName, String unit, double unitPrice, int availableStock, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.availableStock = availableStock;
        this.quantity = quantity;
    }

    public double lineTotal() {
        return unitPrice * quantity;
    }
}
