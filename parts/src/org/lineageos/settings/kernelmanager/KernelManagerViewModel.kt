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
    val minFreq: String = "",
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
        val curGov = prefs.getString(PREF_GOVERNOR, null)
            ?: KernelManagerUtils.getCurrentGovernor(firstPolicy)

        val clusters = discovered.map { cluster ->
            val freqs = KernelManagerUtils.getAvailableFrequencies(cluster.policy) ?: emptyList()
            ClusterState(
                policy = cluster.policy,
                label = cluster.label,
                availableFreqs = freqs,
                minFreq = prefs.getString(minKey(cluster.policy), null)
                    ?: KernelManagerUtils.getCurrentMinFrequency(cluster.policy),
                maxFreq = prefs.getString(maxKey(cluster.policy), null)
                    ?: KernelManagerUtils.getCurrentMaxFrequency(cluster.policy),
            )
        }

        _state.update {
            it.copy(
                availableGovernors = govs,
                currentGovernor = curGov,
                clusters = clusters,
                applyOnBoot = prefs.getBoolean(PREF_APPLY_ON_BOOT, false),
            )
        }
    }

    fun updateGovernor(gov: String) {
        _state.update { it.copy(currentGovernor = gov) }
    }

    fun updateClusterMinFreq(policy: Int, freq: String) {
        _state.update { s ->
            s.copy(clusters = s.clusters.map {
                if (it.policy == policy) it.copy(minFreq = freq) else it
            })
        }
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
            KernelManagerUtils.setFrequencyRange(cluster.policy, cluster.minFreq, cluster.maxFreq)
            editor.putString(minKey(cluster.policy), cluster.minFreq)
            editor.putString(maxKey(cluster.policy), cluster.maxFreq)
        }
        editor.apply()
    }

    fun resetSettings() {
        val keepApplyOnBoot = prefs.getBoolean(PREF_APPLY_ON_BOOT, false)
        prefs.edit().clear().putBoolean(PREF_APPLY_ON_BOOT, keepApplyOnBoot).apply()
        KernelManagerUtils.resetToDefaults()

        // Update state from known defaults instead of reading back from sysfs,
        // because the walt governor may immediately override scaling_min_freq
        // with its own floor value.
        val discovered = KernelManagerUtils.getClusters()
        val clusters = discovered.map { cluster ->
            val freqs = KernelManagerUtils.getAvailableFrequencies(cluster.policy) ?: emptyList()
            ClusterState(
                policy = cluster.policy,
                label = cluster.label,
                availableFreqs = freqs,
                minFreq = freqs.firstOrNull() ?: KernelManagerUtils.DEFAULT_MIN_FREQ,
                maxFreq = freqs.lastOrNull() ?: "0",
            )
        }

        _state.update {
            it.copy(
                availableGovernors = KernelManagerUtils.getAvailableGovernors(),
                currentGovernor = KernelManagerUtils.DEFAULT_GOVERNOR,
                clusters = clusters,
                applyOnBoot = keepApplyOnBoot,
            )
        }
    }

    companion object {
        const val PREFS_NAME = "kernel_manager_settings"
        const val PREF_GOVERNOR = "governor"
        const val PREF_APPLY_ON_BOOT = "apply_on_boot"

        fun minKey(policy: Int) = "policy${policy}_min_freq"
        fun maxKey(policy: Int) = "policy${policy}_max_freq"
    }
}
