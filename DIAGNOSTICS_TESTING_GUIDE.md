# Forge Diagnostics Testing Guide

This guide explains how to test the OBD-II hardware communication layer of the application, both automatically using the simulator and manually using real USB hardware.

## 1. Local Automated Testing (Simulator)

The `tools/ecu-simulator` directory contains a Python-based ECU simulator. This simulator is used to test our `SimulatorConnection.kt` class without requiring physical hardware.

We have written an automated test called `MockSimulatorConnectionTest.kt` inside the `app/src/test/.../hardware/` folder. This test automatically stands up a mock socket server that behaves exactly like the Python ECU simulator, and verifies that our Android code can parse OBD-II responses perfectly.

You can run these tests via the terminal:
```bash
./gradlew :app:testDebugUnitTest --tests "*MockSimulatorConnectionTest*"
```

The complete Python simulator runner covers configuration, OBD, and UDS suites:
```bash
python tools/ecu-simulator/run_tests.py
```

The dependency-free virtual ELM327 harness can also be tested without Android, Linux CAN, or a phone:
```bash
python -m unittest discover -s test/hil_emulator -p "test*.py"
```

## 2. Real-World Testing (USB-OTG / Innova Scanner)

Since you have an Innova 5610rs and a USB-OTG cable, you can perform live hardware testing.

I have built a new, hidden screen in the application specifically for this purpose called the **Hardware Test Screen**.

### How to use it:
1. Compile and install the app on your phone:
   ```bash
   ./gradlew :app:installDebug
   ```
2. Open the app on your phone.
3. Use the sidebar navigation menu to select **"Hardware Test"**.
4. Plug your USB-OTG cable into the phone, and plug the other end into the OBD-II port on your vehicle.
5. On the screen, you will see a list of connected USB devices. Tap your OBD-II cable in the list.
6. Android will display a popup asking for USB permission. **Tap Allow**.
7. The status text should change to **Connected to USB Hardware successfully!**

### Read-only commands first
Use read-only commands before any clearing or actuator operation:

- **RPM:** `01 0C`
- **Vehicle speed:** `01 0D`
- **Coolant temperature:** `01 05`
- **VIN:** `09 02`
- **Stored DTCs:** `03`
- **Pending DTCs:** `07`

Do not treat an adapter connection as proof that a vehicle response is valid. The app should display the raw response and parsed result only after validating the response framing.

## 3. Bidirectional Controls

Standard OBD-II does not define manufacturer-specific bidirectional controls such as commanding a cooling fan. Do not add or send such commands without vehicle-specific documentation, a controlled test fixture, and an explicit safety review.
