package com.example.automationapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class PacketMonitorVpnService extends VpnService {
    private static final String TAG = "VpnService";
    private ParcelFileDescriptor vpnInterface;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("vpn_channel", "VPN Service",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // Start as foreground service (required on Android 13)
        Notification notification = new NotificationCompat.Builder(this, "vpn_channel")
                .setContentTitle("VPN Service")
                .setContentText("Monitoring packets")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        startForeground(1, notification);

        // Set up VPN interface (inspired by PCAPdroid)
        Builder builder = new Builder();
        builder.setSession("PacketMonitor");
        builder.addAddress("10.215.173.1", 32); // Local VPN IP
        builder.addRoute("0.0.0.0", 0);        // Route all traffic
        builder.addDnsServer("8.8.8.8");      // Google DNS
        builder.setMtu(1500);                  // Maximum transmission unit

        try {
            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN");
                stopSelf();
                return START_NOT_STICKY;
            }
            Log.d(TAG, "VPN established successfully");
            new Thread(this::monitorPackets).start();
        } catch (Exception e) {
            Log.e(TAG, "Error establishing VPN", e);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private void monitorPackets() {
        try {
            FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
            FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());
            byte[] buffer = new byte[32767];

            while (true) {
                int length = in.read(buffer);
                if (length > 0) {
                    ByteBuffer packet = ByteBuffer.wrap(buffer, 0, length);
                    if (isFleetleryPacket(packet)) {
                        Log.d(TAG, "Fleetlery packet detected");
                        sendBroadcast(new Intent("NEW_ORDER_DETECTED"));
                    }
                    out.write(buffer, 0, length); // Forward packet
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error monitoring packets", e);
        }
    }

    private boolean isFleetleryPacket(ByteBuffer packet) {
        if (packet.limit() < 20) return false; // IP header check
        String srcIp = String.format("%d.%d.%d.%d",
                packet.get(12) & 0xFF, packet.get(13) & 0xFF,
                packet.get(14) & 0xFF, packet.get(15) & 0xFF);
        // Replace with actual Fleetlery server IP after analysis
        return "104.26.3.141".equals(srcIp); // Example IP (e.g., Cloudflare-hosted)
    }

    @Override
    public void onDestroy() {
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing VPN", e);
            }
        }
        super.onDestroy();
    }
}