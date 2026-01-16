package io.github.codehasan.quicksettings.ui;

import static android.content.pm.PackageManager.GET_SERVICES;
import static android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS;
import static io.github.codehasan.quicksettings.util.NullSafety.isNullOrEmpty;
import static io.github.codehasan.quicksettings.util.RootUtil.isRootAvailable;
import static io.github.codehasan.quicksettings.util.RootUtil.isRootGranted;
import static io.github.codehasan.quicksettings.util.RootUtil.setRootGranted;

import android.Manifest;
import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spanned;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.codehasan.quicksettings.R;
import io.github.codehasan.quicksettings.annotations.MinSdk;
import io.github.codehasan.quicksettings.ui.adapter.ServiceAdapter;
import io.github.codehasan.quicksettings.ui.model.ServiceItem;

public class MainActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private MaterialToolbar toolbar;
    private MaterialButton btnRoot;
    private MaterialButton btnWriteSettings;
    private ServiceAdapter adapter;

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

        adapter = new ServiceAdapter(getServices());
        recyclerView.setAdapter(adapter);

        toolbar = findViewById(R.id.toolbar);
        setupSearchBar();

        btnRoot.setOnClickListener(v -> {
            executor.execute(() -> {
                if (isRootAvailable()) {
                    setRootGranted(this, true);
                    handler.post(() -> setPermissionGrantStatus(btnRoot, true));
                } else {
                    setRootGranted(this, false);
                    handler.post(() -> {
                        setPermissionGrantStatus(btnRoot, false);
                        Toast.makeText(
                                this,
                                R.string.no_root_access,
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

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

        setPermissionGrantStatus(btnRoot, isRootGranted(this));
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

    private void setupSearchBar() {
        MenuItem menuItem = toolbar.getMenu().findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        if (searchView == null) return;

        searchView.setQueryHint(getString(R.string.search) + "...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.getFilter().filter(newText);
                }
                return true;
            }
        });
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
        PackageManager pm = getPackageManager();
        List<ServiceItem> serviceItems = new ArrayList<>();
        ServiceInfo[] services = null;

        try {
            PackageInfo pi = pm.getPackageInfo(getPackageName(),
                    GET_SERVICES | MATCH_DISABLED_COMPONENTS);
            services = pi.services;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        if (isNullOrEmpty(services)) return serviceItems;

        for (ServiceInfo service : services) {
            if (!service.permission.equals("android.permission.BIND_QUICK_SETTINGS_TILE"))
                continue;

            ServiceItem serviceItem = new ServiceItem();

            serviceItem.component = new ComponentName(service.packageName, service.name);
            serviceItem.title = getString(service.labelRes);
            serviceItem.description = getString(service.descriptionRes);
            serviceItem.icon = service.icon;

            int state = pm.getComponentEnabledSetting(serviceItem.component);
            boolean isExplicitlyEnabled = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            boolean isDefaultAndManifestEnabled = (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) && service.enabled;
            serviceItem.enabled = isExplicitlyEnabled || isDefaultAndManifestEnabled;

            if (!service.enabled) {
                serviceItem.reason = getString(R.string.not_supported);
                try {
                    Class<?> serviceClass = Class.forName(service.name);
                    if (serviceClass.isAnnotationPresent(MinSdk.class)) {
                        MinSdk annotation = serviceClass.getAnnotation(MinSdk.class);
                        serviceItem.reason = getString(R.string.not_supported_below_api, annotation.value());
                    }
                } catch (Exception ignored) {
                }
            }

            serviceItems.add(serviceItem);
        }
        return serviceItems;
    }

    private boolean hasSecureSettingsPermission() {
        return checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
