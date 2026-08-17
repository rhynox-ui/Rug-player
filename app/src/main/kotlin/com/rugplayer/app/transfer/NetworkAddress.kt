package com.rugplayer.app.transfer

import java.net.Inet4Address
import java.net.NetworkInterface

/** Finds this device's local Wi-Fi/LAN address, e.g. "192.168.1.42". */
fun findLocalIpAddress(): String? = runCatching {
    NetworkInterface.getNetworkInterfaces()?.asSequence()
        ?.filter { it.isUp && !it.isLoopback }
        ?.flatMap { it.inetAddresses.asSequence() }
        ?.filterIsInstance<Inet4Address>()
        ?.firstOrNull { !it.isLoopbackAddress }
        ?.hostAddress
}.getOrNull()
