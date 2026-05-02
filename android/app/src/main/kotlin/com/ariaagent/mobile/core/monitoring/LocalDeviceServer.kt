package com.ariaagent.mobile.core.monitoring

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

/**
 * LocalDeviceServer — Minimal embedded HTTP server for on-device monitoring.
 *
 * Serves ARIA monitoring snapshots from [LocalSnapshotStore] over HTTP on the
 * device's local Wi-Fi address. The web dashboard (or any browser on the same LAN)
 * can connect directly to:
 *
 *   http://{device-ip}:{port}/aria/{endpoint}
 *
 * No new dependencies — uses only Java's [ServerSocket] from the standard library.
 * All responses include CORS headers so any browser origin can connect.
 *
 * Endpoints:
 *   GET /aria/status   — current agent state
 *   GET /aria/thermal  — thermal / temperature level
 *   GET /aria/rl       — RL metrics (Adam, loss, episodes)
 *   GET /aria/lora     — LoRA adapter version history
 *   GET /aria/memory   — embedding store stats
 *   GET /aria/activity — recent action logs (last 50)
 *   GET /aria/modules  — per-module status
 *
 * Started automatically by AgentViewModel.init when the app launches.
 * Default port: 8765.
 *
 * Phase: 16 (Local Monitoring)
 */
object LocalDeviceServer {

    private const val TAG          = "LocalDeviceServer"
    const val DEFAULT_PORT         = 8765

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverJob:    Job?         = null
    private var serverSocket: ServerSocket? = null

    @Volatile var currentPort: Int     = DEFAULT_PORT
        private set
    @Volatile var running:     Boolean  = false
        private set

    // ─── NSD (mDNS) service advertisement ────────────────────────────────────
    private var nsdManager:          NsdManager?                       = null
    private var nsdRegistrationListener: NsdManager.RegistrationListener? = null
    @Volatile var nsdRegistered: Boolean = false
        private set

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    fun start(port: Int = DEFAULT_PORT) {
        if (running) stop()
        currentPort = port
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                running = true
                Log.i(TAG, "listening on port $port — connect at ${serverUrl()}")
                while (isActive) {
                    try {
                        val client = ss.accept()
                        launch { handleClient(client) }
                    } catch (e: IOException) {
                        if (isActive) Log.w(TAG, "accept error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "failed to start on port $port: ${e.message}")
            } finally {
                running = false
            }
        }
    }

    fun stop() {
        stopNsd()
        serverJob?.cancel()
        serverJob = null
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        running = false
        Log.d(TAG, "stopped")
    }

    // ─── NSD helpers ─────────────────────────────────────────────────────────

    /**
     * Advertise this server via mDNS so the web dashboard can auto-discover
     * the device IP without the user typing it manually.
     *
     * Service type : `_aria._tcp`
     * Service name : `ARIA-Device` (Android appends digits if name is taken)
     *
     * Safe to call multiple times — unregisters the previous instance first.
     * Requires ACCESS_WIFI_STATE permission (already declared for Wi-Fi IP lookup).
     */
    fun startNsd(context: Context) {
        stopNsd()
        try {
            val nm = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
            nsdManager = nm

            val info = NsdServiceInfo().apply {
                serviceName = "ARIA-Device"
                serviceType = "_aria._tcp"
                port        = currentPort
            }

            val listener = object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(si: NsdServiceInfo, code: Int) {
                    Log.w(TAG, "NSD registration failed: $code")
                    nsdRegistered = false
                }
                override fun onUnregistrationFailed(si: NsdServiceInfo, code: Int) {
                    Log.w(TAG, "NSD unregistration failed: $code")
                }
                override fun onServiceRegistered(si: NsdServiceInfo) {
                    Log.i(TAG, "NSD registered as '${si.serviceName}' on port $currentPort")
                    nsdRegistered = true
                }
                override fun onServiceUnregistered(si: NsdServiceInfo) {
                    Log.d(TAG, "NSD unregistered")
                    nsdRegistered = false
                }
            }
            nsdRegistrationListener = listener
            nm.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.w(TAG, "NSD start failed (non-fatal): ${e.message}")
        }
    }

    fun stopNsd() {
        try {
            nsdRegistrationListener?.let { nsdManager?.unregisterService(it) }
        } catch (_: Exception) {}
        nsdRegistrationListener = null
        nsdManager              = null
        nsdRegistered           = false
    }

    // ─── Device IP ────────────────────────────────────────────────────────────

    /** Returns the device's local Wi-Fi IPv4 address, or 127.0.0.1 as fallback. */
    fun getDeviceIp(): String {
        return try {
            val ifaces = java.net.NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            while (ifaces.hasMoreElements()) {
                val iface = ifaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is java.net.Inet4Address) return addr.hostAddress ?: continue
                }
            }
            "127.0.0.1"
        } catch (_: Exception) { "127.0.0.1" }
    }

    /** Full HTTP URL to the monitoring server (e.g. "http://192.168.1.42:8765"). */
    fun serverUrl(): String = "http://${getDeviceIp()}:$currentPort"

    // ─── Request handler ──────────────────────────────────────────────────────

    private fun handleClient(client: Socket) {
        try {
            client.soTimeout = 3_000
            client.use { sock ->
                val reader = sock.getInputStream().bufferedReader()

                // Read the request line (e.g. "GET /aria/status HTTP/1.1")
                val requestLine = reader.readLine()?.trim() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val method = parts[0]
                val path   = parts[1].substringBefore("?")

                // Handle CORS pre-flight
                if (method == "OPTIONS") {
                    sendRaw(sock, "HTTP/1.0 204 No Content\r\nAccess-Control-Allow-Origin: *\r\nAccess-Control-Allow-Headers: *\r\nConnection: close\r\n\r\n")
                    return
                }

                val jsonBody: String = when (path) {
                    "/aria/status"   -> wrap(LocalSnapshotStore.status.toString())
                    "/aria/thermal"  -> wrap(LocalSnapshotStore.thermal.toString())
                    "/aria/rl"       -> wrap(LocalSnapshotStore.rl.toString())
                    "/aria/lora"     -> wrap(LocalSnapshotStore.lora.toString())
                    "/aria/memory"   -> wrap(LocalSnapshotStore.memory.toString())
                    "/aria/activity" -> wrap(LocalSnapshotStore.activity.toString())
                    "/aria/modules"  -> wrap(LocalSnapshotStore.modules.toString())
                    "/health"        -> """{"ok":true,"running":true}"""
                    else             -> """{"ok":false,"error":"not found","path":"$path"}"""
                }

                val bytes = jsonBody.toByteArray(Charsets.UTF_8)
                val header = buildString {
                    append("HTTP/1.0 200 OK\r\n")
                    append("Content-Type: application/json; charset=utf-8\r\n")
                    append("Content-Length: ${bytes.size}\r\n")
                    append("Access-Control-Allow-Origin: *\r\n")
                    append("Access-Control-Allow-Headers: *\r\n")
                    append("Connection: close\r\n")
                    append("\r\n")
                }

                val out = sock.getOutputStream()
                out.write(header.toByteArray(Charsets.UTF_8))
                out.write(bytes)
                out.flush()
            }
        } catch (e: Exception) {
            Log.w(TAG, "client handler error: ${e.message}")
        }
    }

    private fun wrap(data: String) = """{"ok":true,"data":$data}"""

    private fun sendRaw(sock: Socket, response: String) {
        try { sock.getOutputStream().write(response.toByteArray(Charsets.UTF_8)) } catch (_: Exception) {}
    }
}
