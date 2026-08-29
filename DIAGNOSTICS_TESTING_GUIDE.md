# Forge Diagnostics Testing Guide

This guide explains how to test the OBD-II hardware communication layer of the application, both automatically using the simulator and manually using real USB hardware.

## 1. Local Automated Testing (Simulator)

The `tools/ecu-simulator` directory contains a Python-based ECU simulator. This simulator is used to test our `SimulatorConnection.kt` class without requiring physical hardware.

We have written an automated test called `MockSimulatorConnectionTest.kt` inside the `app/src/test/.../hardware/` folder. This test automatically stands up a mock socket server that behaves exactly like the Python ECU simulator, and verifies that our Android code can parse OBD-II responses perfectly.

You can run these tests via the terminal:
```bash
./gradlew :app:testDebugUnitTest --tests "*MockSimulatorConnectionTest*"
```

## 2. Real-World Testing (USB-OTG / Innova Scanner)

Since you have an Innova 5610rs and a USB-OTG cable, you can perform live hardware testing.

I have built a new, hidden screen in the application specifically for this purpose called the **Hardware Test Screen**.

### How to use it:
1. Compile and install the app on your Android device:
   ```bash
   ./gradlew :app:installDebug
   ```
2. Open the app on your phone.
3. Use the sidebar navigation menu to select **"Hardware Test"**.
4. Plug your USB-OTG cable into your phone, and plug the other end into the OBD-II port on your vehicle.
5. On the screen, you will see a list of connected USB devices. Tap your OBD-II cable in the list.
6. Android will display a popup asking for permission to access the USB device. **Tap Allow**.
7. The status text will change to "Connected to USB Hardware successfully!"

### Sending Custom Commands
Once connected, you can use the buttons at the bottom of the screen to send standard OBD-II commands like:
* **Test RPM (01 0C):** Requests current engine RPM.
* **Test VIN (09 02):** Requests the vehicle's VIN.

The terminal window on the screen will print out exactly what bytes were sent out the USB port and what bytes the vehicle returned.

## 3. Extending for Bidirectional Controls
Because standard OBD-II (Service 01) doesn't cover bidirectional controls (like turning on a fan), you will need to reverse-engineer manufacturer-specific commands using your Innova scanner.

**How to capture them:**
1. You will need a CAN bus sniffer (like a Raspberry Pi with a PiCAN hat, or a cheap logic analyzer) connected to the OBD-II port alongside your Innova scanner.
2. Use the Innova scanner to execute a bidirectional command (e.g., "Trigger cooling fan").
3. Look at your CAN bus sniffer logs to see what specific CAN ID and hex payload the Innova scanner sent to the car.
4. Once you have that payload, you can hardcode it into our Android app and send it via the `usbConnection.sendCommand("YOUR_HEX_PAYLOAD")` function inside the app to replicate the exact same behavior!
