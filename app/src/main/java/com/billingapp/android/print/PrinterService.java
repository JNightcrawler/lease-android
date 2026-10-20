package com.billingapp.android.print;

import android.content.Context;

import com.billingapp.android.data.entity.Bill;
import com.billingapp.android.data.entity.BillItem;

import java.util.List;

/** Implemented by each printer path (system Print Framework, Bluetooth thermal). */
public interface PrinterService {
    void printBill(Context context, Bill bill, List<BillItem> items, PrintCallback callback);
}
