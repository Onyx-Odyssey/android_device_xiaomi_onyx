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

package org.lineageos.settings.kernelmanager

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Per-cluster frequency selection held in the UI state. */
data class ClusterState(
    val policy: Int = 0,
    val label: String = "",
    val availableFreqs: List<String> = emptyList(),
    val maxFreq: String = "",
)

data class KernelState(
    val availableGovernors: List<String> = emptyList(),
    val currentGovernor: String = "",
    val clusters: List<ClusterState> = emptyList(),
    val applyOnBoot: Boolean = false,
)

class KernelManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(KernelState())
    val state: StateFlow<KernelState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        val govs = KernelManagerUtils.getAvailableGovernors()
        val discovered = KernelManagerUtils.getClusters()
        val firstPolicy = discovered.firstOrNull()?.policy ?: 0

        val clusters = discovered.map { cluster ->
            ClusterState(
                policy = cluster.policy,
                label = cluster.label,
                availableFreqs = KernelManagerUtils.getAvailableFrequencies(cluster.policy) ?: emptyList(),
                maxFreq = KernelManagerUtils.getCurrentMaxFrequency(cluster.policy),
            )
        }

        _state.update {
            it.copy(
                availableGovernors = govs,
                currentGovernor = KernelManagerUtils.getCurrentGovernor(firstPolicy),
                clusters = clusters,
                applyOnBoot = prefs.getBoolean(PREF_APPLY_ON_BOOT, false),
            )
        }
    }

    fun updateGovernor(gov: String) {
        _state.update { it.copy(currentGovernor = gov) }
    }

    fun updateClusterMaxFreq(policy: Int, freq: String) {
        _state.update { s ->
            s.copy(clusters = s.clusters.map {
                if (it.policy == policy) it.copy(maxFreq = freq) else it
            })
        }
    }

    fun setApplyOnBoot(enabled: Boolean) {
        _state.update { it.copy(applyOnBoot = enabled) }
        prefs.edit().putBoolean(PREF_APPLY_ON_BOOT, enabled).apply()
    }

    fun applySettings() {
        val s = state.value
        KernelManagerUtils.setGovernor(s.currentGovernor)
        val editor = prefs.edit().putString(PREF_GOVERNOR, s.currentGovernor)
        for (cluster in s.clusters) {
            KernelManagerUtils.setMaxFrequency(cluster.policy, cluster.maxFreq)
            editor.putString(maxKey(cluster.policy), cluster.maxFreq)
        }
        editor.apply()

        // Read back so the UI shows what the kernel accepted, not what was requested.
        loadSettings()
    }

    fun resetSettings() {
        val keepApplyOnBoot = prefs.getBoolean(PREF_APPLY_ON_BOOT, false)
        prefs.edit().clear().putBoolean(PREF_APPLY_ON_BOOT, keepApplyOnBoot).apply()
        KernelManagerUtils.resetToDefaults()
        loadSettings()
    }

    companion object {
        const val PREFS_NAME = "kernel_manager_settings"
        const val PREF_GOVERNOR = "governor"
        const val PREF_APPLY_ON_BOOT = "apply_on_boot"

        fun maxKey(policy: Int) = "policy${policy}_max_freq"
    }
}