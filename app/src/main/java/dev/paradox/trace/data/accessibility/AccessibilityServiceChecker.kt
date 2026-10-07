package dev.paradox.trace.data.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager

/**
 * Reads whether Trace's accessibility service is currently enabled by the
 * user. Accessibility can be revoked at any moment from system settings, so
 * the answer is always re-read, never cached.
 */
class AccessibilityServiceChecker(private val context: Context) {

    fun isEnabled(): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        val expected = ComponentName(context, TraceAccessibilityService::class.java).flattenToString()
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.id == expected }
    }
}
