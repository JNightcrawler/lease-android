package com.billingapp.android.print;

/**
 * The flag that selects which printer path is used. See PrinterPreferences for
 * where this is stored, and PrinterServiceFactory for where it's read.
 */
public enum PrinterType {
    // Uses Android's built-in Print Framework (system dialog) - works with any printer
    // already set up on the device: WiFi printers, "print to PDF", etc. No pairing needed.
    SYSTEM_PRINT,

    // Sends raw ESC/POS commands directly to a paired Bluetooth thermal receipt printer.
    // Needs the printer paired via Android's Bluetooth settings first, and a device
    // selected in-app (see PrinterSetupActivity).
    BLUETOOTH_THERMAL
}
