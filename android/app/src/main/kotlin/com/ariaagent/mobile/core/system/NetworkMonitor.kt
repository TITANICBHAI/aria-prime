package com.ariaagent.mobile.core.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

/**
 * NetworkMonitor — lightweight connectivity checker for the on-device agent.
 *
 * Used to:
 *   • Gate web-based tool calls that require an internet connection.
 *   • Surface "OFFLINE" chip in the Dashboard so the user knows immediately
 *     if the agent cannot reach external endpoints.
 *
 * Implementation notes:
 *   - Uses `ConnectivityManager.getNetworkCapabilities()` (API 21+) which is
 *     accurate on Android 10+ (older APIs can be spoofed by VPNs).
 *   - Does NOT use NetworkCallback because the ViewModel polls every 30 s —
 *     a callback would leak if the scope is not cancelled carefully.
 *   - ETHERNET is classified as WIFI for the purposes of the chip label since
 *     both mean "connected and likely high-bandwidth".
 *
 * Phase: Round 13 — GAP_AUDIT §59
 */
object NetworkMonitor {

    private const val TAG = "NetworkMonitor"

    enum class ConnectionType { WIFI, MOBILE, NONE }

    /**
     * Returns the type of active internet connection, or [ConnectionType.NONE] if offline.
     * Safe to call on any thread.
     */
    fun connectionType(context: Context): ConnectionType {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return ConnectionType.NONE
            val caps = cm.getNetworkCapabilities(network) ?: return ConnectionType.NONE
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)      -> ConnectionType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)  -> ConnectionType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)  -> ConnectionType.MOBILE
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)       -> ConnectionType.MOBILE
                else                                                        -> ConnectionType.NONE
            }
        }.getOrElse {
            Log.w(TAG, "connectionType() failed: ${it.message}")
            ConnectionType.NONE
        }
    }

    /** Returns true if any internet-capable network is active. */
    fun isOnline(context: Context): Boolean = connectionType(context) != ConnectionType.NONE
}
