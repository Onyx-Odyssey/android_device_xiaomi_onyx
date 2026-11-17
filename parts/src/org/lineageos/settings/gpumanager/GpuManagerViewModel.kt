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

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GpuManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val gpuUtils = GpuManagerUtils()
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class GpuState(
        val gpuModel: String = "",
        val currentMaxFreq: String = "",
        val availableFrequencies: List<String> = emptyList(),
        val currentFreq: String = "",
        val busyPercentage: String = "",
        val temperature: String = "",
        val thermalPowerLevel: String = "",
        val busSplit: Boolean = false,
        val applyOnBoot: Boolean = false
    )

    private val _uiState = MutableStateFlow(GpuState())
    val uiState: StateFlow<GpuState> = _uiState.asStateFlow()

    init {
        loadInitialState()
        startPeriodicUpdates()
    }

    private fun loadInitialState() {
        _uiState.update {
            it.copy(
                gpuModel = gpuUtils.getGpuModel(),
                currentMaxFreq = gpuUtils.getCurrentMaxFrequency(),
                availableFrequencies = gpuUtils.getAvailableFrequencies()?.toList() ?: emptyList(),
                busSplit = gpuUtils.getBusSplit(),
                applyOnBoot = prefs.getBoolean(PREF_APPLY_ON_BOOT, false)
            )
        }
        updateDynamicInfo()
    }

    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            while (true) {
                updateDynamicInfo()
                delay(2000)
            }
        }
    }

    private fun updateDynamicInfo() {
        _uiState.update {
            it.copy(
                currentFreq = gpuUtils.getCurrentFrequency(),
                busyPercentage = gpuUtils.getGpuBusyPercentage(),
                temperature = gpuUtils.getGpuTemperature(),
                thermalPowerLevel = gpuUtils.getThermalPowerLevel()
            )
        }
    }

    fun setMaxFrequency(freq: String) {
        _uiState.update { it.copy(currentMaxFreq = freq) }
    }

    fun setBusSplit(enabled: Boolean) {
        _uiState.update { it.copy(busSplit = enabled) }
    }

    fun setApplyOnBoot(enabled: Boolean) {
        _uiState.update { it.copy(applyOnBoot = enabled) }
        prefs.edit().putBoolean(PREF_APPLY_ON_BOOT, enabled).apply()
    }

    fun applySettings() {
        val state = _uiState.value
        gpuUtils.setMaxFrequency(state.currentMaxFreq)
        gpuUtils.setBusSplit(state.busSplit)

        prefs.edit()
            .putString(PREF_MAX_FREQ, state.currentMaxFreq)
            .putBoolean(PREF_BUS_SPLIT, state.busSplit)
            .apply()

        // Read back so the UI shows what the kernel accepted, not what was requested.
        loadInitialState()
    }

    fun resetSettings() {
        val keepApplyOnBoot = prefs.getBoolean(PREF_APPLY_ON_BOOT, false)
        prefs.edit().clear().putBoolean(PREF_APPLY_ON_BOOT, keepApplyOnBoot).apply()
        gpuUtils.resetToDefaults()
        loadInitialState()
    }

    companion object {
        const val PREFS_NAME = "gpu_manager_settings"
        const val PREF_MAX_FREQ = "max_freq"
        const val PREF_BUS_SPLIT = "bus_split"
        const val PREF_APPLY_ON_BOOT = "apply_on_boot"
    }
}