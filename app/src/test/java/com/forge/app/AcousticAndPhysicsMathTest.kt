// Copyright (c) 2026 Michael Mario Johnson. All Rights Reserved.
// Proprietary and Confidential.
// This file is part of Forge Agentic Diagnostics.
// Unauthorized copying of this file, via any medium is strictly prohibited.

package com.forge.app

import com.forge.app.services.OpenManusAgentService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AcousticAndPhysicsMathTest {

    private lateinit var agentService: OpenManusAgentService

    @Before
    fun setup() {
        agentService = OpenManusAgentService()
    }

    @Test
    fun testSaeJ1979PidMathFormulas() {
        // SAE J1979 PID 010C (RPM): ((A * 256) + B) / 4
        val rpmA = 0x0B
        val rpmB = 0xB8
        val calculatedRpm = ((rpmA * 256) + rpmB) / 4
        assertEquals(750, calculatedRpm)

        // SAE J1979 PID 0105 (Coolant Temp): A - 40
        val ectA = 132 // 132 - 40 = 92°C
        val calculatedEct = ectA - 40
        assertEquals(92, calculatedEct)

        // SAE J1979 PID 0106 (Short Term Fuel Trim): (A - 128) * 100 / 128
        val stftRaw = 147 // (147 - 128) * 100 / 128 = +14.84%
        val stftPct = ((stftRaw - 128) * 100.0) / 128.0
        assertEquals(14.84, stftPct, 0.01)

        // SAE J1979 PID 0110 (MAF Air Flow Rate): ((A * 256) + B) / 100
        val mafA = 0x01
        val mafB = 0x44 // (256 + 68) / 100 = 3.24 g/s
        val calculatedMaf = ((mafA * 256) + mafB) / 100.0
        assertEquals(3.24, calculatedMaf, 0.001)
    }

    @Test
    fun testVolumetricEfficiencyAcrossRpmBands() {
        // Throttled Idle condition: 750 RPM, 3.0L engine, 3.24 g/s MAF, 24°C IAT
        val idleVe = agentService.calculateVolumetricEfficiency(3.24, 750, 3.0, 24.0)
        assertTrue("Throttled Idle VE should be within 10% to 25%", idleVe in 10.0..25.0)

        // Naturally Aspirated WOT condition: 6000 RPM, 3.0L engine, 160.0 g/s MAF, 20°C IAT
        val wotVe = agentService.calculateVolumetricEfficiency(160.0, 6000, 3.0, 20.0)
        assertTrue("WOT VE should be within 80% to 100%", wotVe in 80.0..100.0)

        // Wide Open Throttle (WOT) Turbocharged condition: 5500 RPM, 2.0L engine, 195.0 g/s MAF (Boosted ~1.2 bar), 45°C IAT
        val boostedVe = agentService.calculateVolumetricEfficiency(195.0, 5500, 2.0, 45.0)
        assertTrue("Boosted VE should exceed 100%", boostedVe > 100.0)
    }

    @Test
    fun testAcousticHarmonicsAcrossEngineSpeeds() {
        // Test at 600 RPM (Idle): Fundamental crankshaft = 10.0 Hz
        val idle600Cam = agentService.calculateAcousticDominantHarmonic(600, 5.0)
        assertTrue(idle600Cam.contains("0.5x"))

        val idle600Rod = agentService.calculateAcousticDominantHarmonic(600, 10.0)
        assertTrue(idle600Rod.contains("1.0x"))

        // Test at 1800 RPM (Cruise): Fundamental crankshaft = 30.0 Hz
        val cruise1800Cam = agentService.calculateAcousticDominantHarmonic(1800, 15.0)
        assertTrue(cruise1800Cam.contains("0.5x"))

        val cruise1800Rod = agentService.calculateAcousticDominantHarmonic(1800, 30.0)
        assertTrue(cruise1800Rod.contains("1.0x"))

        // Test at 3600 RPM (High Load): Fundamental crankshaft = 60.0 Hz
        val highLoadCam = agentService.calculateAcousticDominantHarmonic(3600, 30.0)
        assertTrue(highLoadCam.contains("0.5x"))

        val highLoadRod = agentService.calculateAcousticDominantHarmonic(3600, 60.0)
        assertTrue(highLoadRod.contains("1.0x"))
    }

    @Test
    fun testFftFrequencyResolutionPhysics() {
        val sampleRate = 44100 // 44.1 kHz standard audio
        val nFft = 2048 // 2048-point FFT
        val binResolution = sampleRate.toDouble() / nFft // ~21.53 Hz per bin

        val highResNFft = 8192 // 8192-point FFT
        val highResBinResolution = sampleRate.toDouble() / highResNFft // ~5.38 Hz per bin

        assertTrue(highResBinResolution < binResolution)
        assertTrue("High-resolution FFT must resolve < 6 Hz camshaft peaks", highResBinResolution <= 6.0)
    }
}
