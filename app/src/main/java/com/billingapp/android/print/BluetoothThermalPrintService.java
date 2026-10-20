package com.billingapp.android.print;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.billingapp.android.data.entity.Bill;
import com.billingapp.android.data.entity.BillItem;
import com.billingapp.android.util.AppExecutors;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Sends a bill as raw ESC/POS commands to a paired Bluetooth thermal receipt
 * printer. Requires the printer to already be paired via Android's Bluetooth
 * settings, and a device selected in PrinterSetupActivity (see PrinterPreferences).
 */
public class BluetoothThermalPrintService implements PrinterService {

    // Standard Serial Port Profile (SPP) UUID - virtually all Bluetooth thermal
    // receipt printers use this for their RFCOMM service.
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    @Override
    public void printBill(Context context, Bill bill, List<BillItem> items, PrintCallback callback) {
        PrinterPreferences prefs = new PrinterPreferences(context);
        String address = prefs.getBluetoothDeviceAddress();

        if (address == null) {
            callback.onError("No Bluetooth printer selected. Go to Printer Setup and choose a paired device.");
            return;
        }

        if (!hasBluetoothConnectPermission(context)) {
            callback.onError("Bluetooth permission not granted. Go to Printer Setup to grant it.");
            return;
        }

        AppExecutors.getInstance().diskIO(() -> {
            BluetoothSocket socket = null;
            try {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter == null) {
                    postError(callback, "This device has no Bluetooth adapter");
                    return;
                }
                if (!adapter.isEnabled()) {
                    postError(callback, "Bluetooth is turned off. Turn it on and try again.");
                    return;
                }

                BluetoothDevice device = adapter.getRemoteDevice(address);
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();

                byte[] receipt = buildReceipt(bill, items);
                OutputStream out = socket.getOutputStream();
                out.write(receipt);
                out.flush();

                AppExecutors.getInstance().mainThread(callback::onSuccess);
            } catch (IOException e) {
                postError(callback, "Couldn't connect to printer: " + (e.getMessage() != null ? e.getMessage() : "unknown error"));
            } catch (SecurityException e) {
                postError(callback, "Bluetooth permission denied");
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                        // Nothing useful to do if closing the socket fails.
                    }
                }
            }
        });
    }

    private byte[] buildReceipt(Bill bill, List<BillItem> items) {
        EscPosBuilder receipt = new EscPosBuilder()
                .init()
                .alignCenter()
                .doubleHeight(true)
                .bold(true)
                .line("RECEIPT")
                .doubleHeight(false)
                .bold(false)
                .line(bill.displayNumber())
                .line(DATE_FORMAT.format(new java.util.Date(bill.createdAt)))
                .divider()
                .alignLeft();

        if (bill.customerName != null) {
            receipt.line("Customer: " + bill.customerName);
        }
        if (bill.customerPhone != null) {
            receipt.line("Phone: " + bill.customerPhone);
        }
        if (bill.customerName != null || bill.customerPhone != null) {
            receipt.divider();
        }

        for (BillItem item : items) {
            receipt.line(item.productName);
            receipt.line(String.format(Locale.getDefault(), "  %d %s x %.2f = %.2f",
                    item.quantity, item.unit, item.unitPrice, item.lineTotal));
        }

        receipt.divider()
                .bold(true)
                .line(String.format(Locale.getDefault(), "TOTAL: %.2f", bill.totalAmount))
                .bold(false)
                .feed(1)
                .alignCenter()
                .line("Thank you!")
                .feed(3)
                .cutPaper();

        return receipt.build();
    }

    private boolean hasBluetoothConnectPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        // Below API 31, BLUETOOTH/BLUETOOTH_ADMIN are normal (install-time) permissions - always granted if declared.
        return true;
    }

    private void postError(PrintCallback callback, String message) {
        AppExecutors.getInstance().mainThread(() -> callback.onError(message));
    }
}
