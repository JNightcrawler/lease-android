package com.billingapp.android.print;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores which printer path is active (the flag) and, for Bluetooth thermal
 * printing, which paired device to use. Configured from PrinterSetupActivity.
 */
public class PrinterPreferences {

    private static final String PREFS_NAME = "printer_prefs";
    private static final String KEY_PRINTER_TYPE = "printer_type";
    private static final String KEY_BT_DEVICE_ADDRESS = "bt_device_address";
    private static final String KEY_BT_DEVICE_NAME = "bt_device_name";

    private final SharedPreferences prefs;

    public PrinterPreferences(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public PrinterType getPrinterType() {
        String stored = prefs.getString(KEY_PRINTER_TYPE, PrinterType.SYSTEM_PRINT.name());
        try {
            return PrinterType.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return PrinterType.SYSTEM_PRINT;
        }
    }

    public void setPrinterType(PrinterType type) {
        prefs.edit().putString(KEY_PRINTER_TYPE, type.name()).apply();
    }

    public String getBluetoothDeviceAddress() {
        return prefs.getString(KEY_BT_DEVICE_ADDRESS, null);
    }

    public String getBluetoothDeviceName() {
        return prefs.getString(KEY_BT_DEVICE_NAME, null);
    }

    public void setBluetoothDevice(String address, String name) {
        prefs.edit()
                .putString(KEY_BT_DEVICE_ADDRESS, address)
                .putString(KEY_BT_DEVICE_NAME, name)
                .apply();
    }

    public boolean hasBluetoothDeviceConfigured() {
        return getBluetoothDeviceAddress() != null;
    }
}
