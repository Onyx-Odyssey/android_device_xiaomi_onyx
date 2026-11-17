/*
 * SPDX-FileCopyrightText: 2020 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.thermal

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.UserHandle
import android.provider.MediaStore
import android.telecom.TelecomManager
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import org.lineageos.settings.R
import org.lineageos.settings.utils.Logging
import org.lineageos.settings.utils.FileUtils
import com.android.settingslib.applications.AppUtils.isBrowserApp

/** Helper utility class for thermal profiles. */
class ThermalUtils
private constructor(
    private val context: Context,
) {
    private val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val serviceIntent = Intent(context, ThermalService::class.java)

    var enabled: Boolean = sharedPrefs.getBoolean(THERMAL_ENABLED, false)
        set(value) {
            if (field == value) return
            field = value
            sharedPrefs.edit().putBoolean(THERMAL_ENABLED, value).apply()
            if (value) {
                startService()
            } else {
                setDefaultThermalProfile()
                stopService()
            }
        }

    var value: String = readValue()
        set(value) {
            if (field == value) return
            field = value
            writeValue(value)
        }

    fun startService() {
        if (enabled) {
            Logging.d(TAG, "startService")
            context.startServiceAsUser(serviceIntent, UserHandle.CURRENT)
        }
    }

    fun stopService() {
        Logging.d(TAG, "stopService")
        context.stopService(serviceIntent)
    }

    private fun writeValue(value: String) {
        Logging.d(TAG, "writing pref value: $value")
        sharedPrefs.edit().putString(THERMAL_CONTROL, value).apply()
    }

    private fun readValue(): String = sharedPrefs.getString(THERMAL_CONTROL, null) ?: DEFAULT_VALUE

    fun writePackage(packageName: String, mode: Int) {
        Logging.d(TAG, "writePackage: $packageName -> $mode")
        var newValue = value.replace("$packageName,", "")
        val modes = newValue.split(":").toMutableList()
        if (mode < modes.size) {
            modes[mode] += "$packageName,"
        }
        value = modes.joinToString(":")
    }

    fun getStateForPackage(packageName: String): ThermalState {
        val modes = value.split(":")
        return ThermalState.values().find { state -> 
            state.id < modes.size && modes[state.id].contains("$packageName,") 
        } ?: getDefaultStateForPackage(packageName)
    }

    fun resetProfiles() {
        Logging.d(TAG, "resetProfiles")
        value = DEFAULT_VALUE
    }

    fun setDefaultThermalProfile() {
        Logging.d(TAG, "setDefaultThermalProfile")
        FileUtils.writeLine(THERMAL_SCONFIG, ThermalState.DEFAULT.config)
    }

    fun setThermalProfile(packageName: String) {
        if (packageName.isEmpty()) {
            Logging.d(TAG, "setThermalProfile: packageName is empty")
            return
        }
        val state = getStateForPackage(packageName)
        Logging.d(TAG, "setThermalProfile: $packageName -> $state")
        FileUtils.writeLine(THERMAL_SCONFIG, state.config)
    }

    private fun getDefaultStateForPackage(packageName: String): ThermalState {
        runCatching { context.packageManager.getApplicationInfo(packageName, 0) }
            .onSuccess {
                when (it.category) {
                    ApplicationInfo.CATEGORY_GAME -> return ThermalState.GAMING
                    ApplicationInfo.CATEGORY_VIDEO -> return ThermalState.VIDEO
                    ApplicationInfo.CATEGORY_MAPS -> return ThermalState.NAVIGATION
                }
            }
            .onFailure {
                return ThermalState.DEFAULT
            }

        return when {
            GAME_PACKAGES.contains(packageName) -> ThermalState.GAMING
            NAVIGATION_PACKAGES.contains(packageName) -> ThermalState.NAVIGATION
            VIDEO_CALL_PACKAGES.contains(packageName) -> ThermalState.VIDEOCALL
            BENCHMARKING_APPS.contains(packageName) -> ThermalState.BENCHMARK
            context.getSystemService(TelecomManager::class.java)
                ?.defaultDialerPackage == packageName -> ThermalState.DIALER
            isBrowserApp(context, packageName, UserHandle.myUserId()) -> ThermalState.BROWSER
            isCameraApp(packageName) -> ThermalState.CAMERA
            else -> ThermalState.DEFAULT
        }
    }

    private fun isCameraApp(packageName: String): Boolean {
        val cameraIntent =
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).setPackage(packageName)
        val activities =
            context.packageManager.queryIntentActivitiesAsUser(
                cameraIntent,
                PackageManager.MATCH_ALL,
                UserHandle.myUserId(),
            )
        return activities.any { it.activityInfo != null }
    }

    enum class ThermalState(
        val id: Int,
        val config: String,
        val prefix: String,
        @StringRes val label: Int,
    ) {
        // Scenario ids from onyx's own thermald map: 6 NOLIMITS, 5 PHONE,
        // 15 CAMERA, 18 TGAME, 10 NAVIGATION, 14 VIDEOCHAT, 11 VIDEO.
        BENCHMARK(0, "6", "thermal.benchmark=", R.string.thermal_benchmark),
        BROWSER(1, "0", "thermal.browser=", R.string.thermal_browser),
        CAMERA(2, "15", "thermal.camera=", R.string.thermal_camera),
        DIALER(3, "5", "thermal.dialer=", R.string.thermal_dialer),
        GAMING(4, "18", "thermal.gaming=", R.string.thermal_gaming),
        NAVIGATION(5, "10", "thermal.navigation=", R.string.thermal_navigation),
        VIDEOCALL(6, "14", "thermal.streaming=", R.string.thermal_streaming),
        VIDEO(7, "11", "thermal.video=", R.string.thermal_video),
        DEFAULT(8, "0", "thermal.default=", R.string.thermal_default),
    }

    companion object {
        private const val TAG = "ThermalUtils"
        private const val THERMAL_CONTROL = "thermal_control_v2"
        private const val THERMAL_ENABLED = "thermal_enabled"

        private const val THERMAL_SCONFIG = "/sys/class/thermal/thermal_message/sconfig"

        private val DEFAULT_VALUE = ThermalState.values().joinToString(":") { "${it.prefix}," }

        private val GAME_PACKAGES = arrayOf(
            "com.activision.callofduty.shooter",
            "com.activision.callofduty.warzone",
            "com.dts.freefiremax",
            "com.dts.freefireth",
            "com.epicgames.fortnite",
            "com.garena.game.codm",
            "com.pubg.imobile",
            "com.pubg.krmobile",
            "com.pubg.newstate",
            "com.rekoo.pubgm",
            "com.tencent.ig",
            "com.tencent.tmgp.cod",
            "com.tencent.tmgp.pubgmhd",
            "com.vng.pubgmobile",
            "com.garena.game.kgid",
            "com.garena.game.kgth",
            "com.garena.game.kgtw",
            "com.garena.game.kgvn",
            "com.levelinfinite.sgameGlobal",
            "com.mobile.legends",
            "com.ngame.allstar.eu",
            "com.riotgames.league.teamfighttactics",
            "com.riotgames.league.wildrift",
            "com.tencent.tmgp.sgame",
            "com.HoYoverse.Nap",
            "com.HoYoverse.hkrpgoversea",
            "com.carxtech.sr",
            "com.gameloft.android.ANMP.GloftA8HM",
            "com.gameloft.android.ANMP.GloftA9HM",
            "com.kurogame.gplay.punishing.grayraven.en",
            "com.kurogame.haru.hero",
            "com.kurogame.wutheringwaves.global",
            "com.miHoYo.GenshinImpact",
            "com.miHoYo.Nap",
            "com.miHoYo.Yuanshen",
            "com.miHoYo.bh3.mihoyo",
            "com.miHoYo.bh3global",
            "com.miHoYo.bh3oversea",
            "com.miHoYo.hkrpg",
            "com.mojang.minecraftpe",
            "com.roblox.client",
            "com.supercell.brawlstars",
            "com.supercell.clashofclans",
            "com.supercell.clashroyale",
            "com.explusalpha.Snes9xPlus",
            "com.fastemulator.gba",
            "com.retroarch",
            "com.retroarch.aarch64",
            "com.winlator",
            "com.winlator.mod",
            "com.winlator.vanilla",
            "org.citra.citra_emu",
            "org.dolphinemu.dolphinemu",
            "org.ppsspp.ppsspp",
            "org.ppsspp.ppssppgold",
            "org.yuzu.yuzu_emu",
            "org.yuzu.yuzu_emu.ea",
            "skyline.emu",
            "xyz.aethersx2.android",
        )

        private val NAVIGATION_PACKAGES = arrayOf(
            "com.google.android.apps.maps",
            "com.google.android.apps.mapslite",
            "com.waze",
        )
        private val VIDEO_CALL_PACKAGES = arrayOf(
            "com.google.android.apps.tachyon",
            "us.zoom.videomeetings",
            "com.microsoft.teams",
            "com.skype.raider",
        )
        private val BENCHMARKING_APPS = arrayOf(
            "com.primatelabs.geekbench5",
            "com.primatelabs.geekbench6",
            "com.antutu.ABenchMark",
            "com.futuremark.dmandroid.application",
            "com.futuremark.pcmark.android.benchmark",
            "com.glbenchmark.glbenchmark27",
            "com.texts.throttlebench",
            "skynet.cputhrottlingtest",
        )

        @Volatile private var instance: ThermalUtils? = null

        @JvmStatic
        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: ThermalUtils(context.applicationContext).also { instance = it }
            }
    }
}
