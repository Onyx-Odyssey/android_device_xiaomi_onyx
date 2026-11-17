package org.lineageos.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.lineageos.settings.gpumanager.GpuManagerUtils
import org.lineageos.settings.gpumanager.GpuManagerViewModel
import org.lineageos.settings.kernelmanager.KernelManagerUtils
import org.lineageos.settings.kernelmanager.KernelManagerViewModel

class ApplyOnBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                applyKernelSettings(context)
                applyGpuSettings(context)
            }
        }
    }

    private fun applyKernelSettings(context: Context) {
        val prefs = context.getSharedPreferences(
            KernelManagerViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KernelManagerViewModel.PREF_APPLY_ON_BOOT, false)) return

        prefs.getString(KernelManagerViewModel.PREF_GOVERNOR, null)
            ?.let { KernelManagerUtils.setGovernor(it) }

        // Restore every cluster that has a stored max. Clusters are discovered
        // at runtime so this covers onyx's four policies (0, 2, 5, 7).
        for (cluster in KernelManagerUtils.getClusters()) {
            prefs.getString(KernelManagerViewModel.maxKey(cluster.policy), null)
                ?.let { KernelManagerUtils.setMaxFrequency(cluster.policy, it) }
        }
    }

    private fun applyGpuSettings(context: Context) {
        val prefs = context.getSharedPreferences(
            GpuManagerViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(GpuManagerViewModel.PREF_APPLY_ON_BOOT, false)) return

        val utils = GpuManagerUtils()
        prefs.getString(GpuManagerViewModel.PREF_MAX_FREQ, null)
            ?.let { utils.setMaxFrequency(it) }
        utils.setBusSplit(prefs.getBoolean(GpuManagerViewModel.PREF_BUS_SPLIT, true))
    }
}
