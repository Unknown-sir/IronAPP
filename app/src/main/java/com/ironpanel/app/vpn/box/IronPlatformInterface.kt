package com.ironpanel.app.vpn.box

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.ironpanel.app.vpn.IronVpnService
import com.ironpanel.libbox.BridgeOptions
import com.ironpanel.libbox.BridgeSession
import com.ironpanel.libbox.ConnectionOwner
import com.ironpanel.libbox.InterfaceUpdateListener
import com.ironpanel.libbox.Libbox
import com.ironpanel.libbox.LocalDNSTransport
import com.ironpanel.libbox.NeighborUpdateListener
import com.ironpanel.libbox.NetworkInterface as BoxInterface
import com.ironpanel.libbox.NetworkInterfaceIterator
import com.ironpanel.libbox.PlatformInterface
import com.ironpanel.libbox.PlatformUser
import com.ironpanel.libbox.ShellSession
import com.ironpanel.libbox.StringIterator
import com.ironpanel.libbox.TunOptions
import com.ironpanel.libbox.WIFIState
import java.net.NetworkInterface as JNetworkInterface

/**
 * Host-side implementation of sing-box's mobile PlatformInterface.
 * Modeled on the official SFA client; trimmed to what IronAPP uses
 * (TUN + protect + interfaces). No root required.
 *
 * NOTE: VpnService.Builder is an inner class, so TUN construction lives in
 * IronVpnService (a VpnService subclass) and this class only delegates.
 */
class IronPlatformInterface(
    private val vpn: IronVpnService,
    private val appContext: Context,
) : PlatformInterface {

    private val connectivity: ConnectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var tunName: String = ""
    private var netCallback: ConnectivityManager.NetworkCallback? = null
    private var netListener: InterfaceUpdateListener? = null

    // ---------- socket protect / TUN ----------

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

    override fun autoDetectInterfaceControl(fd: Int) {
        vpn.protect(fd)
    }

    override fun openTun(options: TunOptions): Int = vpn.createTun(options)

    // ---------- connection owner ----------

    override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String,
        sourcePort: Int,
        destinationAddress: String,
        destinationPort: Int,
    ): ConnectionOwner {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) error("ironapp: procfs mode")
        val uid = connectivity.getConnectionOwnerUid(
            ipProtocol,
            java.net.InetSocketAddress(sourceAddress, sourcePort),
            java.net.InetSocketAddress(destinationAddress, destinationPort),
        )
        if (uid == Process.INVALID_UID) error("ironapp: owner not found")
        val packages = appContext.packageManager.getPackagesForUid(uid)
        return ConnectionOwner().apply {
            userId = uid
            userName = packages?.firstOrNull() ?: ""
            setAndroidPackageNames(StringArray((packages?.toList() ?: emptyList()).iterator()))
        }
    }

    // ---------- network interfaces / monitor ----------

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        closeDefaultInterfaceMonitor(listener)
        netListener = listener
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = pushDefault()
            override fun onLost(network: Network) = pushDefault()
            override fun onCapabilitiesChanged(
                network: Network,
                caps: NetworkCapabilities,
            ) = pushDefault()
        }
        netCallback = callback
        try {
            connectivity.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                callback,
            )
        } catch (_: Exception) {
        }
        pushDefault()
    }

    private fun pushDefault() {
        val listener = netListener ?: return
        try {
            val network = connectivity.activeNetwork ?: run {
                listener.updateDefaultInterface("", -1, false, false)
                return
            }
            val props = connectivity.getLinkProperties(network)
            val name = props?.interfaceName ?: ""
            var index = -1
            try {
                index = JNetworkInterface.getByName(name)?.index ?: -1
            } catch (_: Exception) {
            }
            listener.updateDefaultInterface(name, index, false, false)
        } catch (_: Exception) {
        }
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        netListener = null
        try {
            netCallback?.let { connectivity.unregisterNetworkCallback(it) }
        } catch (_: Exception) {
        }
        netCallback = null
    }

    @SuppressLint("MissingPermission")
    override fun getInterfaces(): NetworkInterfaceIterator {
        val out = mutableListOf<BoxInterface>()
        try {
            val javaIfaces = JNetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            for (network in connectivity.allNetworks) {
                try {
                    val props = connectivity.getLinkProperties(network) ?: continue
                    val caps = connectivity.getNetworkCapabilities(network) ?: continue
                    val name = props.interfaceName ?: continue
                    val jface = javaIfaces.find { it.name == name } ?: continue
                    if (name == tunName) continue
                    val box = BoxInterface()
                    box.name = name
                    box.dnsServer = StringArray(
                        props.dnsServers.mapNotNull { it.hostAddress }.iterator()
                    )
                    box.gateway = StringArray(
                        props.routes.filter { it.destination?.prefixLength == 0 }
                            .mapNotNull { it.gateway }
                            .filterNot { it.isAnyLocalAddress }
                            .mapNotNull { it.hostAddress }
                            .iterator()
                    )
                    box.type = when {
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Libbox.InterfaceTypeWIFI
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Libbox.InterfaceTypeCellular
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Libbox.InterfaceTypeEthernet
                        else -> Libbox.InterfaceTypeOther
                    }
                    try {
                        box.index = jface.index
                    } catch (_: Exception) {
                    }
                    try {
                        box.mtu = jface.mtu
                    } catch (_: Exception) {
                    }
                    box.addresses = StringArray(
                        jface.interfaceAddresses.map { it.address.hostAddress + "/" + it.networkPrefixLength }
                            .iterator()
                    )
                    var flags = 0
                    if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        flags = flags or android.system.OsConstants.IFF_UP or
                            android.system.OsConstants.IFF_RUNNING
                    }
                    if (jface.isLoopback) flags = flags or android.system.OsConstants.IFF_LOOPBACK
                    if (jface.supportsMulticast()) flags = flags or android.system.OsConstants.IFF_MULTICAST
                    box.flags = flags
                    box.metered =
                        !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    out.add(box)
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
        return InterfaceArray(out)
    }

    override fun underNetworkExtension(): Boolean = false

    override fun includeAllNetworks(): Boolean = false

    override fun clearDNSCache() {
    }

    override fun readWIFIState(): WIFIState? = null

    override fun localDNSTransport(): LocalDNSTransport? = null

    // ---------- notifications ----------

    override fun sendNotification(notification: com.ironpanel.libbox.Notification) {
        try {
            val mgr = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (mgr.getNotificationChannel("ironapp_core") == null) {
                    mgr.createNotificationChannel(
                        NotificationChannel(
                            "ironapp_core", "IronAPP core",
                            NotificationManager.IMPORTANCE_LOW
                        )
                    )
                }
            }
            val built = NotificationCompat.Builder(appContext, "ironapp_core")
                .setContentTitle(notification.title)
                .setContentText(notification.body)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOnlyAlertOnce(true)
                .build()
            mgr.notify(notification.identifier, notification.typeID, built)
        } catch (_: Exception) {
        }
    }

    override fun cancelNotification(identifier: String, typeID: Int) {
        try {
            val mgr = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.cancel(identifier, typeID)
        } catch (_: Exception) {
        }
    }

    // ---------- unused platform capabilities ----------

    override fun startNeighborMonitor(listener: NeighborUpdateListener?) {
    }

    override fun closeNeighborMonitor(listener: NeighborUpdateListener?) {
    }

    override fun usePlatformShell(): Boolean = false

    override fun checkPlatformShell() {
    }

    override fun openShellSession(
        user: PlatformUser?,
        command: String?,
        environ: StringIterator?,
        term: String?,
        rows: Int,
        cols: Int,
    ): ShellSession = error("ironapp: shell not supported")

    override fun readSystemSSHHostKey(): String = ""

    override fun lookupSFTPServer(): String = ""

    override fun tailscaleHostname(): String = ""

    override fun usePlatformBridge(): Boolean = false

    override fun createBridge(options: BridgeOptions?): BridgeSession =
        error("ironapp: bridge not supported")

    override fun lookupUser(username: String?): PlatformUser =
        PlatformUser().apply {
            this.username = username ?: ""
            uid = Process.myUid()
            gid = Process.myUid()
            homeDir = ""
        }

    override fun registerMyInterface(name: String?) {
        tunName = name ?: ""
    }
}

/** Holder for the platform TUN fd (closed by the service on stop). */
object BoxTun {
    var fd: android.os.ParcelFileDescriptor? = null
    fun close() {
        try {
            fd?.close()
        } catch (_: Exception) {
        }
        fd = null
    }
}
