package com.example.automationapp;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityResultLauncher<Intent> vpnPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the Activity Result Launcher for VPN permission
        vpnPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "VPN permission granted");
                        startVpnService();
                    } else {
                        Log.e(TAG, "VPN permission denied");
                        Toast.makeText(this, "VPN permission is required", Toast.LENGTH_LONG).show();
                    }
                }
        );

        Button startVpnButton = findViewById(R.id.start_vpn_button);
        startVpnButton.setOnClickListener(v -> {
            Intent prepareIntent = VpnService.prepare(this);
            if (prepareIntent != null) {
                Log.d(TAG, "Requesting VPN permission");
                vpnPermissionLauncher.launch(prepareIntent);
            } else {
                Log.d(TAG, "VPN already prepared, starting service");
                startVpnService();
            }
        });

        Button enableAccessibilityButton = findViewById(R.id.enable_accessibility_button);
        enableAccessibilityButton.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(this, "Please enable AutomationApp Accessibility Service", Toast.LENGTH_LONG).show();
        });
    }

    private void startVpnService() {
        Intent intent = new Intent(this, PacketMonitorVpnService.class);
        Log.d(TAG, "Starting VPN service");
        startService(intent);
    }
}