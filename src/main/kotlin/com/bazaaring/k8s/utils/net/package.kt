package com.bazaaring.k8s.utils.net

import java.net.NetworkInterface

fun findLocalAddresses(): List<String> {
    return runCatching { NetworkInterface.getNetworkInterfaces() }
        .getOrNull()
        ?.asSequence()
        ?.flatMap { inf ->
            inf.inetAddresses.asSequence().mapNotNull { address ->
                if (!address.isLoopbackAddress && address.isSiteLocalAddress) {
                    address.hostAddress
                } else {
                    null
                }
            }
        }
        ?.toList().orEmpty()
}
