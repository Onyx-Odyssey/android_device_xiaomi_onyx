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

import java.io.File

/**
 * CPU frequency / governor control.
 *
 * Clusters are discovered at runtime from /sys/devices/system/cpu/cpufreq/policy*,
 * so this works on any number of clusters (onyx has four: 0, 2, 5, 7) instead of
 * assuming a fixed two-cluster topology.
 */
object KernelManagerUtils {
    internal const val DEFAULT_GOVERNOR = "walt"
    internal const val DEFAULT_MIN_FREQ = "480000"

    private const val CPU_BASE_PATH = "/sys/devices/system/cpu/cpufreq/policy"
    private const val SCALING_GOVERNOR = "/scaling_governor"
    private const val SCALING_MIN_FREQ = "/scaling_min_freq"
    private const val SCALING_MAX_FREQ = "/scaling_max_freq"
    private const val SCALING_AVAILABLE_GOVERNORS = "/scaling_available_governors"
    private const val SCALING_AVAILABLE_FREQUENCIES = "/scaling_available_frequencies"
    private const val CPU_CPUFREQ_DIR = "/sys/devices/system/cpu/cpufreq"

    /** One CPU cluster: its policy number, member cores and a display label. */
    data class Cluster(
        val policy: Int,
        val cores: List<Int>,
        val label: String,
    )

    /**
     * Discover every cpufreq policy present on the device, ordered by policy number.
     * Falls back to a single policy 0 entry if the directory cannot be read.
     */
    fun getClusters(): List<Cluster> {
        val policies = runCatching {
            File(CPU_CPUFREQ_DIR)
                .listFiles { f -> f.isDirectory && f.name.startsWith("policy") }
                ?.mapNotNull { it.name.removePrefix("policy").toIntOrNull() }
                ?.sorted()
        }.getOrNull().orEmpty()

        if (policies.isEmpty()) {
            return listOf(Cluster(0, listOf(0), "Cluster 0"))
        }

        return policies.map { policy ->
            val cores = runCatching {
                File("$CPU_BASE_PATH$policy/related_cpus").readText()
                    .trim()
                    .split(Regex("\\s+"))
                    .mapNotNull { it.toIntOrNull() }
            }.getOrDefault(emptyList())

            Cluster(policy, cores, labelFor(cores, policy, policy == policies.last()))
        }
    }

    /**
     * Build a human label from the cores in the cluster. The single-core
     * top cluster is Prime, the lowest Little, anything between Big.
     */
    private fun labelFor(cores: List<Int>, policy: Int, isTop: Boolean): String {
        if (cores.isEmpty()) return "Cluster $policy"
        val first = cores.first()
        val last = cores.last()
        val coreRange = if (cores.size == 1) "CPU $first" else "CPU $first-$last"
        val kind = when {
            first == 0 -> "Little"
            isTop && cores.size == 1 -> "Prime"
            else -> "Big"
        }
        return "$kind ($coreRange)"
    }

    fun getAvailableGovernors(): List<String> {
        val clusters = getClusters()
        val firstPolicy = clusters.firstOrNull()?.policy ?: 0
        return runCatching {
            readFile(CPU_BASE_PATH + firstPolicy + SCALING_AVAILABLE_GOVERNORS)
                .trim().split(Regex("\\s+"))
        }.getOrDefault(
            listOf(DEFAULT_GOVERNOR, "schedutil", "performance", "powersave", "ondemand", "conservative")
        )
    }

    fun getAvailableFrequencies(cluster: Int): List<String>? {
        return runCatching {
            readFile(CPU_BASE_PATH + cluster + SCALING_AVAILABLE_FREQUENCIES)
                .trim().split(Regex("\\s+"))
        }.getOrNull()
    }

    fun getCurrentGovernor(cluster: Int): String {
        return runCatching {
            readFile(CPU_BASE_PATH + cluster + SCALING_GOVERNOR).trim()
        }.getOrDefault(DEFAULT_GOVERNOR)
    }

    fun getCurrentMinFrequency(cluster: Int): String {
        return runCatching {
            readFile(CPU_BASE_PATH + cluster + SCALING_MIN_FREQ).trim()
        }.getOrDefault(DEFAULT_MIN_FREQ)
    }

    fun getCurrentMaxFrequency(cluster: Int): String {
        return runCatching {
            readFile(CPU_BASE_PATH + cluster + SCALING_MAX_FREQ).trim()
        }.getOrDefault(getAvailableFrequencies(cluster)?.lastOrNull() ?: "0")
    }

    fun setGovernor(governor: String) {
        for (cluster in getClusters()) {
            runCatching { writeFile(CPU_BASE_PATH + cluster.policy + SCALING_GOVERNOR, governor) }
        }
    }

    fun setFrequencyRange(cluster: Int, minFreq: String, maxFreq: String) {
        runCatching {
            // Write max before min to avoid the kernel rejecting min > current_max
            writeFile(CPU_BASE_PATH + cluster + SCALING_MAX_FREQ, maxFreq)
            writeFile(CPU_BASE_PATH + cluster + SCALING_MIN_FREQ, minFreq)
        }
    }

    fun resetToDefaults() {
        setGovernor(DEFAULT_GOVERNOR)
        for (cluster in getClusters()) {
            val frequencies = getAvailableFrequencies(cluster.policy) ?: continue
            setFrequencyRange(cluster.policy, frequencies.firstOrNull() ?: DEFAULT_MIN_FREQ, frequencies.last())
        }
    }

    private fun readFile(path: String): String {
        return File(path).readText()
    }

    private fun writeFile(path: String, value: String) {
        File(path).writeText(value)
    }
}
