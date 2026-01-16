package io.github.codehasan.quicksettings.ui;

import static io.github.codehasan.quicksettings.util.NullSafety.isNullOrEmpty;
import static io.github.codehasan.quicksettings.util.RootUtil.isRootAvailable;

import android.Manifest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.text.Spanned;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import io.github.codehasan.quicksettings.R;
import io.github.codehasan.quicksettings.ui.adapter.ServiceAdapter;
import io.github.codehasan.quicksettings.ui.model.ServiceItem;

public class MainActivity extends AppCompatActivity {

    private MaterialButton btnRoot;
    private MaterialButton btnWriteSettings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnRoot = findViewById(R.id.btn_root);
        btnWriteSettings = findViewById(R.id.btn_write_settings);

        RecyclerView recyclerView = findViewById(R.id.services);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ServiceAdapter adapter = new ServiceAdapter(getServices());
        recyclerView.setAdapter(adapter);

        btnRoot.setOnClickListener(v -> isRootAvailable());
        btnWriteSettings.setOnClickListener(v -> {
            if (hasSecureSettingsPermission()) {
                onResume();
            } else {
                showWriteSettingsGuide();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        setPermissionGrantStatus(btnRoot, isRootAvailable());
        setPermissionGrantStatus(btnWriteSettings, hasSecureSettingsPermission());
    }

    private void setPermissionGrantStatus(MaterialButton button, boolean grantStatus) {
        if (grantStatus) {
            button.setEnabled(false);
            button.setText(R.string.allowed);
        } else {
            button.setEnabled(true);
            button.setText(R.string.allow);
        }
    }

    private void showWriteSettingsGuide() {
        Spanned html = HtmlCompat.fromHtml(
                getString(R.string.write_secure_settings_guide),
                HtmlCompat.FROM_HTML_MODE_COMPACT);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.write_settings)
                .setMessage(html)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private List<ServiceItem> getServices() {
        List<ServiceItem> serviceItems = new ArrayList<>();
        ServiceInfo[] services = null;

        try {
            PackageInfo pi = getPackageManager()
                    .getPackageInfo(getPackageName(), PackageManager.GET_SERVICES);
            services = pi.services;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        if (!isNullOrEmpty(services)) {
            for (ServiceInfo serviceInfo : services) {
                if (serviceInfo.permission.equals("android.permission.BIND_QUICK_SETTINGS_TILE")) {
                    ServiceItem item = new ServiceItem();
                    item.serviceClass = serviceInfo.name;
                    item.title = getString(serviceInfo.labelRes);
                    item.description = getString(serviceInfo.labelRes);
                    item.icon = serviceInfo.icon;
                    item.isActive = serviceInfo.isEnabled();
                    serviceItems.add(item);
                }
            }
        }
        return serviceItems;
    }

    private boolean hasSecureSettingsPermission() {
        return checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
