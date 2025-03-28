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
import java.nio.ByteBuffer;

public class PacketMonitorVpnService extends VpnService {
    private static final String TAG = "PacketMonitorVpnService";
    private ParcelFileDescriptor vpnInterface;
    private Thread vpnThread;
    private volatile boolean running = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand called");

        // Notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channel");
            NotificationChannel channel = new NotificationChannel(
                    "vpn_channel",
                    "VPN Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            } else {
                Log.e(TAG, "NotificationManager is null");
                return START_NOT_STICKY;
            }
        }

        // Notification
        Log.d(TAG, "Building notification");
        Notification notification = new NotificationCompat.Builder(this, "vpn_channel")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("VPN Running")
                .setContentText("Monitoring network traffic")
                .setOngoing(true)
                .build();

        try {
            Log.d(TAG, "Starting foreground service");
            startForeground(1, notification);
            Log.e(TAG, "Foreground service started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground service", e);
            return START_NOT_STICKY;
        }

        // VPN configuration (mimic PCAPdroid)
        Log.d(TAG, "Configuring VPN");
        Builder builder = new Builder();
        builder.setSession("PacketMonitorVPN");
        builder.addAddress("10.215.173.1", 24); // Subnet like PCAPdroid
        builder.addRoute("10.215.173.0", 24);   // Route virtual subnet only
        builder.addDnsServer("8.8.8.8");
        builder.addDnsServer("1.1.1.1");

        try {
            Log.d(TAG, "Establishing VPN");
            vpnInterface = builder.establish();
            if (vpnInterface != null) {
                Log.e(TAG, "VPN established successfully");
                startMonitoring();
            } else {
                Log.e(TAG, "VPN interface is null");
                stopSelf();
                return START_NOT_STICKY;
            }
        } catch (Exception e) {
            Log.e(TAG, "VPN establishment failed", e);
            stopSelf();
            return START_NOT_STICKY;
        }

        Log.e(TAG, "onStartCommand completed");
        return START_STICKY;
    }

    private void startMonitoring() {
        Log.e(TAG, "Starting monitoring thread");
        running = true;
        vpnThread = new Thread(() -> {
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
                Log.d(TAG, "Opening TUN streams");
                in = new FileInputStream(vpnInterface.getFileDescriptor());
                out = new FileOutputStream(vpnInterface.getFileDescriptor());
                ByteBuffer packet = ByteBuffer.allocate(32767);

                Log.e(TAG, "Monitoring loop started");
                int packetCount = 0;
                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        int length = in.read(packet.array());
                        if (length > 0) {
                            packet.limit(length);
                            packetCount++;
                            if (packetCount % 50 == 0) {
                                Log.d(TAG, "Packet #" + packetCount + " read, length: " + length);
                            }
                            if (isFleetleryPacket(packet)) {
                                Log.e(TAG, "Fleetlery packet detected");
                                sendBroadcast(new Intent("NEW_ORDER_DETECTED"));
                            }
                            out.write(packet.array(), 0, length);
                            if (packetCount % 50 == 0) {
                                Log.d(TAG, "Packet #" + packetCount + " written, length: " + length);
                            }
                            packet.clear();
                        } else if (length < 0) {
                            Log.w(TAG, "End of stream reached");
                            break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Packet processing error", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Monitoring thread failed", e);
            } finally {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing streams", e);
                }
                Log.e(TAG, "Monitoring thread stopped");
            }
        });
        vpnThread.start();
        Log.e(TAG, "Monitoring thread launched");
    }

    private boolean isFleetleryPacket(ByteBuffer packet) {
        if (packet.limit() < 20) return false;
        int ipVersion = (packet.get(0) & 0xF0) >> 4;
        if (ipVersion != 4) return false;

        String srcIp = String.format("%d.%d.%d.%d",
                packet.get(12) & 0xFF, packet.get(13) & 0xFF,
                packet.get(14) & 0xFF, packet.get(15) & 0xFF);
        return "104.26.3.141".equals(srcIp); // Update with actual IP
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy called");
        running = false;
        if (vpnThread != null) {
            vpnThread.interrupt();
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
                Log.d(TAG, "VPN interface closed");
            } catch (Exception e) {
                Log.e(TAG, "Error closing VPN interface", e);
            }
        }
        stopForeground(true);
        super.onDestroy();
    }
}