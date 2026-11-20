package org.lineageos.settings.deviceextras.ui

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.BatteryManager
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import java.text.Collator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.lineageos.settings.charging.ChargingLimiter
import org.lineageos.settings.Constants
import org.lineageos.settings.otg.OTGModeSwitch
import org.lineageos.settings.otg.OTGTileService
import org.lineageos.settings.R
import org.lineageos.settings.thermallimiter.ThermalLimiter
import org.lineageos.settings.thermallimiter.ThermalLimiterTileService

data class InstalledApp(val packageName: String, val label: String, val icon: Bitmap?)

data class DeviceExtrasState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val saving: Boolean = false,
    val settingsRevision: Long = 0,
    val chargingSupported: Boolean = false,
    val chargingEnabled: Boolean = true,
    val chargingProfile: String = "mid",
    val thermalSupported: Boolean = false,
    val thermalEnabled: Boolean = false,
    val thermalStatus: String = "",
    val otgSupported: Boolean = false,
    val otgEnabled: Boolean = false,
    val temperature: Float? = null,
    val warm: Int = ThermalLimiter.TEMP_DEFAULT_WARM,
    val high: Int = ThermalLimiter.TEMP_DEFAULT_HIGH,
    val critical: Int = ThermalLimiter.TEMP_DEFAULT_CRITICAL,
    val selectedApps: Set<String> = emptySet(),
    val apps: List<InstalledApp> = emptyList(),
    val appsLoading: Boolean = false,
    val appsFailed: Boolean = false,
)

class DeviceExtrasViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)
    private val mutableState = MutableStateFlow(DeviceExtrasState())
    val state = mutableState.asStateFlow()
    private val mutableMessages = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val messages = mutableMessages.asSharedFlow()
    private val settingsLock = Mutex()
    private var capabilities: Triple<Boolean, Boolean, Boolean>? = null
    private var appsJob: Job? = null
    private var appsLoaded = false

    init {
        viewModelScope.launch { refresh() }
    }

    suspend fun refresh() {
        settingsLock.withLock {
            try {
                refreshLocked()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (!mutableState.value.loadFailed) Log.e("DeviceExtras", "Could not read settings", exception)
                mutableState.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun retryRefresh() {
        viewModelScope.launch { refresh() }
    }

    private suspend fun refreshLocked() {
        val snapshot = withContext(Dispatchers.IO) {
            val support = capabilities ?: Triple(
                ChargingLimiter.isSupported(), ThermalLimiter.isSupported(), OTGModeSwitch.isSupported(),
            ).also { capabilities = it }
            val warm = prefs.getInt(ThermalLimiter.KEY_TEMP_WARM, ThermalLimiter.TEMP_DEFAULT_WARM)
                .coerceIn(ThermalLimiter.TEMP_MIN, ThermalLimiter.TEMP_MAX)
            val high = maxOf(warm + 1,
                prefs.getInt(ThermalLimiter.KEY_TEMP_HIGH, ThermalLimiter.TEMP_DEFAULT_HIGH)
                    .coerceAtMost(ThermalLimiter.TEMP_MAX))
            val critical = maxOf(high + 1,
                prefs.getInt(ThermalLimiter.KEY_TEMP_CRITICAL, ThermalLimiter.TEMP_DEFAULT_CRITICAL)
                    .coerceAtMost(ThermalLimiter.TEMP_MAX + 2))
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temperature = battery?.takeIf { it.hasExtra(BatteryManager.EXTRA_TEMPERATURE) }
                ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)?.div(10f)
                ?.takeIf { it in -30f..100f }
            DeviceExtrasState(
                loading = false,
                chargingSupported = support.first,
                chargingEnabled = ChargingLimiter.isEnabled(context),
                chargingProfile = ChargingLimiter.getProfile(context),
                thermalSupported = support.second,
                thermalEnabled = prefs.getBoolean(Constants.KEY_THERMAL_LIMITER, false),
                otgSupported = support.third,
                otgEnabled = support.third && OTGModeSwitch.isCurrentlyEnabled(context),
                temperature = temperature,
                warm = warm,
                high = high,
                critical = critical,
                selectedApps = ThermalLimiter.getSelectedApps(context).toSet(),
            )
        }
        val status = ThermalLimiter.getStatusSummary(context)
        mutableState.update { current ->
            snapshot.copy(
                saving = current.saving,
                settingsRevision = current.settingsRevision,
                thermalStatus = status,
                apps = current.apps,
                appsLoading = current.appsLoading,
                appsFailed = current.appsFailed,
            )
        }
    }

    private fun change(
        error: Int = R.string.device_setting_error,
        optimistic: ((DeviceExtrasState) -> DeviceExtrasState)? = null,
        afterSuccess: () -> Unit = {},
        write: () -> Boolean,
    ) {
        if (mutableState.value.saving || mutableState.value.loadFailed) return
        val previousState = mutableState.value
        mutableState.update { current ->
            val updated = optimistic?.invoke(current) ?: current
            updated.copy(saving = true)
        }
        viewModelScope.launch {
            settingsLock.withLock {
                try {
                    if (withContext(Dispatchers.IO) { write() }) {
                        afterSuccess()
                        refreshLocked()
                    } else {
                        mutableState.value = previousState.copy(saving = false)
                        mutableMessages.tryEmit(error)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (exception: Exception) {
                    mutableState.value = previousState.copy(saving = false)
                    mutableMessages.tryEmit(error)
                } finally {
                    mutableState.update { it.copy(saving = false, settingsRevision = it.settingsRevision + 1) }
                }
            }
        }
    }

    fun setChargingEnabled(enabled: Boolean) = change(
        error = R.string.charging_limiter_error,
        optimistic = { it.copy(chargingEnabled = enabled) },
    ) {
        ChargingLimiter.setEnabled(context, enabled)
    }

    fun setChargingProfile(profile: String) = change(
        error = R.string.charging_limiter_error,
        optimistic = { it.copy(chargingProfile = profile) },
    ) {
        ChargingLimiter.setProfile(context, profile)
    }

    fun setThermalEnabled(enabled: Boolean) = change(
        optimistic = { it.copy(thermalEnabled = enabled) },
        afterSuccess = {
            if (enabled) ThermalLimiter.startMonitoring(context) else ThermalLimiter.stopMonitoring()
            ThermalLimiterTileService.refreshTile()
        },
    ) {
        prefs.edit().putBoolean(Constants.KEY_THERMAL_LIMITER, enabled).apply()
        true
    }

    fun setOtgEnabled(enabled: Boolean) = change(
        optimistic = { it.copy(otgEnabled = enabled) },
        afterSuccess = {
            TileService.requestListeningState(context, ComponentName(context, OTGTileService::class.java))
        },
    ) {
        OTGModeSwitch.setEnabled(context, enabled)
    }

    fun setThreshold(index: Int, value: Int) {
        val current = mutableState.value
        var warm = current.warm
        var high = current.high
        var critical = current.critical
        when (index) {
            0 -> {
                warm = value.coerceIn(35, 60)
                high = maxOf(warm + 1, high.coerceAtMost(60))
                critical = maxOf(high + 1, critical)
            }
            1 -> {
                high = value.coerceIn(36, if (warm == 60) 61 else 60)
                warm = minOf(warm, high - 1)
                critical = maxOf(critical, high + 1)
            }
            2 -> {
                critical = value.coerceIn(37, 62)
                high = minOf(high, critical - 1)
                warm = minOf(warm, high - 1)
            }
            else -> return
        }
        change(
            optimistic = { it.copy(warm = warm, high = high, critical = critical) },
            afterSuccess = { ThermalLimiter.requestImmediateCheck() },
        ) {
            prefs.edit()
                .putInt(ThermalLimiter.KEY_TEMP_WARM, warm)
                .putInt(ThermalLimiter.KEY_TEMP_HIGH, high)
                .putInt(ThermalLimiter.KEY_TEMP_CRITICAL, critical)
                .apply()
            true
        }
    }

    fun resetThresholds() = change(
        optimistic = {
            it.copy(
                warm = ThermalLimiter.TEMP_DEFAULT_WARM,
                high = ThermalLimiter.TEMP_DEFAULT_HIGH,
                critical = ThermalLimiter.TEMP_DEFAULT_CRITICAL,
            )
        },
        afterSuccess = { ThermalLimiter.requestImmediateCheck() },
    ) {
        prefs.edit()
            .putInt(ThermalLimiter.KEY_TEMP_WARM, ThermalLimiter.TEMP_DEFAULT_WARM)
            .putInt(ThermalLimiter.KEY_TEMP_HIGH, ThermalLimiter.TEMP_DEFAULT_HIGH)
            .putInt(ThermalLimiter.KEY_TEMP_CRITICAL, ThermalLimiter.TEMP_DEFAULT_CRITICAL)
            .apply()
        true
    }

    fun selectApp(packageName: String, selected: Boolean) = change(
        optimistic = { current ->
            val set = current.selectedApps.toMutableSet()
            if (selected) set.add(packageName) else set.remove(packageName)
            current.copy(selectedApps = set)
        },
    ) {
        val packages = ThermalLimiter.getSelectedApps(context).toMutableSet()
        if (selected) packages.add(packageName) else packages.remove(packageName)
        ThermalLimiter.setSelectedApps(context, packages)
        true
    }

    fun loadApps() {
        if (appsLoaded || appsJob?.isActive == true) return
        mutableState.update { it.copy(appsLoading = true, appsFailed = false) }
        appsJob = viewModelScope.launch {
            try {
                val entries = withContext(Dispatchers.IO) {
                    val pm = context.packageManager
                    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    val iconSize = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)
                    val collator = Collator.getInstance()
                    pm.queryIntentActivities(launcher, 0)
                        .mapNotNull { it.activityInfo?.applicationInfo }
                        .distinctBy { it.packageName }
                        .map { info ->
                            val icon = runCatching { info.loadIcon(pm).toBitmap(iconSize, iconSize) }.getOrNull()
                            InstalledApp(info.packageName, info.loadLabel(pm).toString(), icon)
                        }
                        .sortedWith { first, second -> collator.compare(first.label, second.label) }
                }
                appsLoaded = true
                mutableState.update { it.copy(apps = entries, appsFailed = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mutableState.update { it.copy(appsFailed = true) }
            } finally {
                mutableState.update { it.copy(appsLoading = false) }
            }
        }
    }
}
