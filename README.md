### AutomationApp

###### GitHub License Android Version

- AutomationApp is an Android application designed to monitor network traffic for Fleetlery order packets with millisecond precision, automatically accepting orders via an accessibility service. It uses Android’s VpnService API to capture packets locally while maintaining internet connectivity, making it ideal for real-time automation tasks on platforms like Fleetlery.

- Developed and tested on Android 13 (Honor device), this app combines a lightweight VPN service with an accessibility overlay to enhance efficiency for gig workers or automated systems.

---

#### Features

- VPN Packet Monitoring: Captures network traffic via a local TUN interface to detect Fleetlery order packets.
    
- Internet Access: Maintains full device connectivity while monitoring (no remote server required).
    
- Fleetlery Detection: Identifies order packets by source IP (e.g., 104.26.3.141) with millisecond precision.
    
- Accessibility Automation: Triggers automatic actions (e.g., clicking "Accept Order") when orders are detected.
    
- Honor Compatibility: Optimized for Honor devices on Android 13, avoiding OEM network restrictions.
    

---

#### Prerequisites

- Android Device: Android 13 or higher (tested on Honor).
    
- Development Environment:
    
    - Android Studio (latest version recommended).
        
    - JDK 11 or higher.
        
    - Android SDK with API 33+.
        
- Permissions:
    
    - VPN permission (granted via system prompt).
        
    - Accessibility service permission (enabled manually).
        
    - INTERNET and FOREGROUND_SERVICE permissions (in AndroidManifest.xml).
        

---

#### Installation

1. Clone the Repository:
    
    bash
    
    ```bash
    git clone https://github.com/yourusername/AutomationApp.git
    cd AutomationApp
    ```
    
2. Open in Android Studio:
    
    - Launch Android Studio.
        
    - Select "Open an existing project" and choose the AutomationApp directory.
        
3. Configure Fleetlery IP:
    
    - In PacketMonitorVpnService.java, update the isFleetleryPacket() method with the actual Fleetlery server IP:
        
        java
        
        ```java
        return "104.26.3.141".equals(srcIp); // Replace with correct IP
        ```
        
    - Use PCAPdroid or Wireshark to capture the real IP if unknown.
        
4. Build and Deploy:
    
    - Connect your Android device via USB (enable Developer Options and USB Debugging).
        
    - Click Build > Rebuild Project.
        
    - Run the app on your device via Run > Run 'app'.
        
5. Grant Permissions:
    
    - VPN: Tap "Start VPN" in the app, then approve the system prompt.
        
    - Accessibility: Tap "Enable Accessibility," navigate to Settings > Accessibility, and enable AutomationApp Accessibility Service.
        

---

#### Usage

1. Launch the App:
    
    - Open AutomationApp on your device.
        
2. Start the VPN:
    
    - Tap "Start VPN" to activate packet monitoring.
        
    - A persistent notification ("VPN Running") will appear.
        
3. Enable Accessibility:
    
    - Tap "Enable Accessibility" and turn on the service in Settings.
        
    - This allows automatic order acceptance when Fleetlery packets are detected.
        
4. Monitor Fleetlery:
    
    - Use Fleetlery normally—when an order packet arrives, the app logs Fleetlery packet detected and broadcasts NEW_ORDER_DETECTED.
        
    - The accessibility service (if implemented) will auto-click "Accept Order."
        
5. Stop the VPN:
    
    - Close the app or disable the VPN in Settings > Connections > VPN.
        

---

#### Project Structure

```text
AutomationApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/automationapp/
│   │   │   │   ├── MainActivity.java          # App entry point, starts VPN
│   │   │   │   ├── PacketMonitorVpnService.java # VPN service for packet monitoring
│   │   │   │   └── AutoClickAccessibilityService.java # (Optional) Auto-click implementation
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml           # Permissions and service declaration
│   └── build.gradle                          # App dependencies
└── README.md                                 # This file
```

---

#### Technical Details

- VPN Configuration

	- Virtual IP: 10.215.173.1/24 (subnet for TUN interface).
    
	- Routing: addRoute("10.215.173.0", 24) routes only the virtual subnet, preserving internet access via Android’s gateway.
    
	- DNS: Google (8.8.8.8) and Cloudflare (1.1.1.1) for reliable name resolution.
    

- Packet Monitoring

	- Reads packets from the TUN interface (FileInputStream).
    
	- Checks for Fleetlery packets by source IP.
    
	- Writes packets back (FileOutputStream) to maintain connectivity.
    

- Accessibility Service

	- Listens for NEW_ORDER_DETECTED broadcasts.
    
	- (To be implemented) Auto-clicks order acceptance UI elements.
    

---

#### Troubleshooting

- No Internet Access:
    
    - Ensure addRoute("10.215.173.0", 24) is used, not 0.0.0.0/0.
        
    - Test with adb logcat -s PacketMonitorVpnService—look for "Packet #X read/written."
        
    - Try alternative IPs (e.g., 192.168.1.1/24) if Honor conflicts persist.
        
- App Closes Unexpectedly:
    
    - Check adb logcat -s MainActivity for onDestroy called.
        
    - Verify foregroundServiceType="dataSync" in AndroidManifest.xml.
        
- Fleetlery Not Detected:
    
    - Confirm the correct server IP in isFleetleryPacket().
        
    - Use PCAPdroid to capture Fleetlery traffic and update accordingly.
        

---

#### Contributing

1. Fork the repository.
    
2. Create a feature branch (git checkout -b feature-name).
    
3. Commit changes (git commit -m "Add feature").
    
4. Push to your fork (git push origin feature-name).
    
5. Open a Pull Request.
    

---

### License

This project is licensed under the MIT License—see LICENSE for details.

---

#### Acknowledgments

- Inspired by [PCAPdroid](https://github.com/emanuele-f/PCAPdroid) for its robust VPN traffic handling.
    
- Built with assistance from Grok (xAI) for real-time debugging and optimization.
    

---


```text
MIT License

Copyright (c) 2025 Sachin-qgem

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
