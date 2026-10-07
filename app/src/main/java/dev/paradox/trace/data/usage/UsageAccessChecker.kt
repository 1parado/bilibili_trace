package dev.paradox.trace.data.usage

import android.app.AppOpsManager
import android.content.Context
import android.os.Process

/**
 * Checks the special "Usage access" grant (AppOps
 * [AppOpsManager.OPSTR_GET_USAGE_STATS]). This is not a runtime permission;
 * the user toggles it in system settings, so Trace can only observe it.
 */
class UsageAccessChecker(private val context: Context) {

    @Suppress("DEPRECATION") // checkOpNoThrow is the only variant available on API 26-28.
    fun isGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
