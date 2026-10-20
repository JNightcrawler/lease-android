package com.billingapp.android;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.billingapp.android.databinding.ActivityMainBinding;
import com.billingapp.android.ui.admin.ProductListFragment;
import com.billingapp.android.ui.billing.BillingHomeFragment;
import com.billingapp.android.ui.history.BillHistoryFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            showFragment(BillingHomeFragment.newInstance());
            binding.bottomNav.setSelectedItemId(R.id.nav_billing);
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_billing) {
                showFragment(BillingHomeFragment.newInstance());
                return true;
            } else if (id == R.id.nav_admin) {
                showFragment(ProductListFragment.newInstance());
                return true;
            } else if (id == R.id.nav_history) {
                showFragment(BillHistoryFragment.newInstance());
                return true;
            }
            return false;
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
