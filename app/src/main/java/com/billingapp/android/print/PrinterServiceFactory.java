package com.billingapp.android.print;

import android.content.Context;

/** Reads PrinterPreferences and returns whichever PrinterService the flag selects. */
public final class PrinterServiceFactory {

    private PrinterServiceFactory() {
    }

    public static PrinterService get(Context context) {
        PrinterType type = new PrinterPreferences(context).getPrinterType();
        switch (type) {
            case BLUETOOTH_THERMAL:
                return new BluetoothThermalPrintService();
            case SYSTEM_PRINT:
            default:
                return new SystemPrintService();
        }
    }
}
