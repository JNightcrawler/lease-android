package com.billingapp.android.ui.settings;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.billingapp.android.databinding.ActivityPrinterSetupBinding;
import com.billingapp.android.print.PrinterPreferences;
import com.billingapp.android.print.PrinterType;

import java.util.Set;

public class PrinterSetupActivity extends AppCompatActivity {

    private ActivityPrinterSetupBinding binding;
    private PrinterPreferences prefs;
    private String selectedAddress;
    private String selectedName;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    loadPairedDevices();
                } else {
                    binding.statusText.setText("Bluetooth permission is required to list paired printers");
                    binding.statusText.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrinterSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = new PrinterPreferences(getApplicationContext());
        selectedAddress = prefs.getBluetoothDeviceAddress();
        selectedName = prefs.getBluetoothDeviceName();

        boolean isBluetooth = prefs.getPrinterType() == PrinterType.BLUETOOTH_THERMAL;
        binding.radioSystemPrint.setChecked(!isBluetooth);
        binding.radioBluetoothThermal.setChecked(isBluetooth);
        binding.bluetoothSection.setVisibility(isBluetooth ? View.VISIBLE : View.GONE);

        if (isBluetooth) {
            checkPermissionAndLoadDevices();
        }

        binding.printerTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean bluetooth = checkedId == binding.radioBluetoothThermal.getId();
            binding.bluetoothSection.setVisibility(bluetooth ? View.VISIBLE : View.GONE);
            if (bluetooth) {
                checkPermissionAndLoadDevices();
            }
        });

        binding.btnGrantPermission.setOnClickListener(v ->
                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT));

        binding.btnSave.setOnClickListener(v -> save());
    }

    private void checkPermissionAndLoadDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            binding.btnGrantPermission.setVisibility(View.VISIBLE);
            binding.deviceGroup.setVisibility(View.GONE);
            binding.noDevicesText.setVisibility(View.GONE);
            return;
        }
        binding.btnGrantPermission.setVisibility(View.GONE);
        loadPairedDevices();
    }

    private void loadPairedDevices() {
        binding.deviceGroup.removeAllViews();

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            binding.noDevicesText.setText("This device has no Bluetooth adapter");
            binding.noDevicesText.setVisibility(View.VISIBLE);
            return;
        }

        Set<BluetoothDevice> paired;
        try {
            paired = adapter.getBondedDevices();
        } catch (SecurityException e) {
            binding.statusText.setText("Bluetooth permission denied");
            binding.statusText.setVisibility(View.VISIBLE);
            return;
        }

        if (paired.isEmpty()) {
            binding.noDevicesText.setVisibility(View.VISIBLE);
            binding.deviceGroup.setVisibility(View.GONE);
            return;
        }

        binding.noDevicesText.setVisibility(View.GONE);
        binding.deviceGroup.setVisibility(View.VISIBLE);

        for (BluetoothDevice device : paired) {
            RadioButton button = new RadioButton(this);
            String name = device.getName() != null ? device.getName() : device.getAddress();
            button.setText(name);
            button.setTag(device.getAddress());
            button.setChecked(device.getAddress().equals(selectedAddress));
            button.setOnClickListener(v -> {
                selectedAddress = device.getAddress();
                selectedName = name;
            });
            binding.deviceGroup.addView(button);
        }
    }

    private void save() {
        boolean bluetooth = binding.radioBluetoothThermal.isChecked();

        if (bluetooth && selectedAddress == null) {
            binding.statusText.setText("Select a paired printer before saving");
            binding.statusText.setVisibility(View.VISIBLE);
            return;
        }

        prefs.setPrinterType(bluetooth ? PrinterType.BLUETOOTH_THERMAL : PrinterType.SYSTEM_PRINT);
        if (bluetooth) {
            prefs.setBluetoothDevice(selectedAddress, selectedName);
        }

        Toast.makeText(this, "Printer settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
