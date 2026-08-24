package com.forge.app

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sin

class AutomotiveOscilloscopeMathTest {

    object OscilloscopeEngine {
        // Calculate peak-to-peak voltage from sampled waveform
        fun calculateVpp(samples: FloatArray): Float {
            if (samples.isEmpty()) return 0f
            var min = samples[0]
            var max = samples[0]
            for (s in samples) {
                if (s < min) min = s
                if (s > max) max = s
            }
            return max - min
        }

        // Calculate Root Mean Square (RMS) voltage
        fun calculateVrms(samples: FloatArray): Float {
            if (samples.isEmpty()) return 0f
            var sumSq = 0.0
            for (s in samples) {
                sumSq += s * s
            }
            return kotlin.math.sqrt(sumSq / samples.size).toFloat()
        }

        // Calculate PWM Duty Cycle % from digital/square automotive sensor samples (threshold 2.5V)
        fun calculateDutyCycle(samples: FloatArray, highThreshold: Float = 2.5f): Float {
            if (samples.isEmpty()) return 0f
            val highCount = samples.count { it >= highThreshold }
            return (highCount.toFloat() / samples.size) * 100f
        }

        // Generate synthetic Hall effect / Crankshaft 60-2 tooth pattern samples
        fun generateCrankshaftPattern(totalTeeth: Int = 60, missingTeeth: Int = 2, samplesPerTooth: Int = 10): FloatArray {
            val totalSamples = totalTeeth * samplesPerTooth
            val result = FloatArray(totalSamples)
            for (tooth in 0 until totalTeeth) {
                val isMissing = tooth >= (totalTeeth - missingTeeth)
                for (s in 0 until samplesPerTooth) {
                    val index = tooth * samplesPerTooth + s
                    if (isMissing) {
                        result[index] = 0f
                    } else {
                        val angle = (s.toFloat() / samplesPerTooth) * 2 * Math.PI
                        result[index] = (sin(angle) * 5.0).toFloat()
                    }
                }
            }
            return result
        }

        // Detect missing teeth sync gap in crankshaft signal
        fun detectMissingToothGapIndex(samples: FloatArray, thresholdRatio: Float = 1.8f): Int {
            var consecutiveZeros = 0
            var maxGapIndex = -1
            var maxGapLength = 0

            for (i in samples.indices) {
                if (kotlin.math.abs(samples[i]) >= 0.1f) {
                    consecutiveZeros = 0
                    continue
                }

                consecutiveZeros++
                if (consecutiveZeros > maxGapLength) {
                    maxGapLength = consecutiveZeros
                    maxGapIndex = i - consecutiveZeros + 1
                }
            }
            return maxGapIndex
        }
    }

    @Test
    fun testVppAndVrmsCalculation() {
        // Pure sine wave 0V to 5V (peak-to-peak 5.0V)
        val samples = FloatArray(100) { i ->
            (sin(i * 2 * Math.PI / 100) * 2.5 + 2.5).toFloat()
        }

        val vpp = OscilloscopeEngine.calculateVpp(samples)
        assertEquals(5.0f, vpp, 0.05f)

        val vrms = OscilloscopeEngine.calculateVrms(samples)
        // Offset sine RMS: sqrt(Vdc^2 + (Vpeak/sqrt(2))^2) = sqrt(2.5^2 + (2.5/sqrt(2))^2) = sqrt(6.25 + 3.125) = ~3.06V
        assertEquals(3.06f, vrms, 0.1f)
    }

    @Test
    fun testPwmDutyCycle() {
        // Injector signal: 30% ON (0V to 12V automotive pulse), 70% OFF
        val samples = FloatArray(100) { i ->
            if (i < 30) 12.0f else 0.0f
        }

        val dutyCycle = OscilloscopeEngine.calculateDutyCycle(samples, highThreshold = 5.0f)
        assertEquals(30.0f, dutyCycle, 0.01f)
    }

    @Test
    fun testCrankshaft60Minus2MissingToothDetection() {
        val samples = OscilloscopeEngine.generateCrankshaftPattern(totalTeeth = 60, missingTeeth = 2, samplesPerTooth = 10)
        assertEquals(600, samples.size)

        // The last 2 teeth (index 580 to 599) should be 0V gap
        val gapIndex = OscilloscopeEngine.detectMissingToothGapIndex(samples)
        assertEquals(580, gapIndex)
    }

    @Test
    fun testO2SensorSwitchingOscillation() {
        // Upstream Narrowband O2 sensor switching between 0.1V (lean) and 0.9V (rich)
        val samples = FloatArray(50) { i ->
            if (i % 10 < 5) 0.85f else 0.15f
        }
        val vpp = OscilloscopeEngine.calculateVpp(samples)
        assertEquals(0.70f, vpp, 0.01f)
        assertTrue("O2 sensor is actively switching", vpp >= 0.6f)
    }
}
