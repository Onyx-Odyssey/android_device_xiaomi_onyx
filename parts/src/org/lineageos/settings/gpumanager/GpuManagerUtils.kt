/*
 * Copyright (C) 2025 KamiKaonashi
 *           (C) 2026 zylhdrXP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package org.lineageos.settings.gpumanager

import java.io.File

class GpuManagerUtils {

    companion object {
        private const val GPU_BASE_PATH = "/sys/class/kgsl/kgsl-3d0"

        // GPU paths
        private const val GPU_MODEL = "$GPU_BASE_PATH/gpu_model"
        private const val GPU_AVAILABLE_FREQUENCIES = "$GPU_BASE_PATH/gpu_available_frequencies"
        private const val GPU_CURRENT_FREQ = "$GPU_BASE_PATH/gpuclk"
        private const val GPU_MAX_FREQ = "$GPU_BASE_PATH/devfreq/max_freq"
        private const val GPU_BUSY_PERCENTAGE = "$GPU_BASE_PATH/gpu_busy_percentage"
        private const val GPU_TEMPERATURE = "$GPU_BASE_PATH/temp"
        private const val GPU_THERMAL_PWRLEVEL = "$GPU_BASE_PATH/thermal_pwrlevel"
        private const val GPU_BUS_SPLIT = "$GPU_BASE_PATH/bus_split"
    }

    fun getGpuModel(): String {
        return runCatching { readFile(GPU_MODEL).trim() }.getOrDefault("Unknown GPU")
    }

    fun getAvailableFrequencies(): Array<String>? {
        return runCatching {
            readFile(GPU_AVAILABLE_FREQUENCIES).trim().split("\\s+".toRegex()).toTypedArray()
        }.getOrNull()
    }

    fun getCurrentFrequency(): String {
        return runCatching { readFile(GPU_CURRENT_FREQ).trim() }.getOrDefault("0")
    }

    fun getCurrentMaxFrequency(): String {
        return runCatching { readFile(GPU_MAX_FREQ).trim() }.getOrDefault("0")
    }

    fun getGpuBusyPercentage(): String {
        return runCatching { readFile(GPU_BUSY_PERCENTAGE).trim() }.getOrDefault("0")
    }

    fun getGpuTemperature(): String {
        return runCatching {
            val rawTemp = readFile(GPU_TEMPERATURE).trim()
            val tempMilliCelsius = rawTemp.toInt()
            val tempCelsius = tempMilliCelsius / 1000.0
            String.format("%.1f", tempCelsius)
        }.getOrDefault("0")
    }

    fun getThermalPowerLevel(): String {
        return runCatching { readFile(GPU_THERMAL_PWRLEVEL).trim() }.getOrDefault("0")
    }

    fun getBusSplit(): Boolean {
        return runCatching { readFile(GPU_BUS_SPLIT).trim() == "1" }.getOrDefault(true)
    }

    fun setMaxFrequency(maxFreq: String) {
        runCatching { writeFile(GPU_MAX_FREQ, maxFreq) }
    }

    fun setBusSplit(enabled: Boolean) {
        runCatching { writeFile(GPU_BUS_SPLIT, if (enabled) "1" else "0") }
    }

    fun resetToDefaults() {
        getAvailableFrequencies()
            ?.mapNotNull { freq -> freq.toLongOrNull()?.let { it to freq } }
            ?.maxByOrNull { it.first }
            ?.second
            ?.let { setMaxFrequency(it) }

        setBusSplit(true)
    }

    private fun readFile(path: String): String {
        return File(path).readText()
    }

    private fun writeFile(path: String, value: String) {
        File(path).writeText(value)
    }
}
