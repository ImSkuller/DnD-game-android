package com.runehearth.app.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.net.Inet4Address
import java.net.NetworkInterface

object NetUtil {
    /** Best guess of this phone's LAN / hotspot IPv4 address (prefers Wi-Fi and hotspot interfaces). */
    fun localAddress(): String? {
        val candidates = runCatching { NetworkInterface.getNetworkInterfaces().toList() }.getOrDefault(emptyList())
            .filter { it.isUp && !it.isLoopback }
            .flatMap { ni -> ni.inetAddresses.toList().filterIsInstance<Inet4Address>().filter { !it.isLoopbackAddress }.map { ni.name to it.hostAddress } }
        return (candidates.firstOrNull { (n, _) -> n.startsWith("wlan") || n.startsWith("ap") || n.startsWith("swlan") } ?: candidates.firstOrNull())?.second
    }
}

data class FoundHost(val key: String, val name: String, val campaignId: String, val host: String, val port: Int)

/** mDNS discovery of hosts on the local network. Only runs while the Join screen is visible. */
class HostDiscovery(context: Context) {
    private val mgr = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _hosts = MutableStateFlow<List<FoundHost>>(emptyList())
    val hosts: StateFlow<List<FoundHost>> = _hosts
    private var listener: NsdManager.DiscoveryListener? = null
    private val queue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    fun start() {
        if (listener != null) return
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(t: String) {}
            override fun onServiceFound(i: NsdServiceInfo) { synchronized(queue) { queue.addLast(i) }; pump() }
            override fun onServiceLost(i: NsdServiceInfo) { _hosts.update { l -> l.filterNot { it.key == i.serviceName } } }
            override fun onDiscoveryStopped(t: String) {}
            override fun onStartDiscoveryFailed(t: String, e: Int) { listener = null }
            override fun onStopDiscoveryFailed(t: String, e: Int) {}
        }
        listener = l
        runCatching { mgr.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    @Suppress("DEPRECATION")
    private fun pump() {
        val next = synchronized(queue) { if (resolving || queue.isEmpty()) return else { resolving = true; queue.removeFirst() } }
        runCatching {
            mgr.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(i: NsdServiceInfo, e: Int) { done() }
                override fun onServiceResolved(i: NsdServiceInfo) {
                    val host = i.host?.hostAddress
                    if (host != null) {
                        val attrs = i.attributes
                        val name = attrs["name"]?.toString(Charsets.UTF_8) ?: i.serviceName
                        val cid = attrs["cid"]?.toString(Charsets.UTF_8) ?: ""
                        _hosts.update { l -> l.filterNot { it.key == i.serviceName } + FoundHost(i.serviceName, name, cid, host, i.port) }
                    }
                    done()
                }
            })
        }.onFailure { done() }
    }

    private fun done() { synchronized(queue) { resolving = false }; pump() }

    fun stop() {
        listener?.let { runCatching { mgr.stopServiceDiscovery(it) } }
        listener = null
        synchronized(queue) { queue.clear(); resolving = false }
    }
}
