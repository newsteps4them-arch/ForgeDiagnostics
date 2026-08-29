# ECU Simulator Test Environment

This directory contains the `ecu-simulator` pulled from `https://github.com/lbenthins/ecu-simulator` as requested by the user.

It is intended strictly for development testing, specifically simulating an ECU connection for our automated testing CI pipelines, and allows us to test OBD-II functionality without needing a physical car or dongle during automated tests.

This directory is isolated and should **not** be included in the Android application bundle or APK.
