package com.mytask.ui.login

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

fun isNetworkAvailable(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false

    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable
fun rememberNetworkAvailable(
    context: Context,
    refreshKey: Int = 0
): Boolean {
    var isOnline by remember(context, refreshKey) {
        mutableStateOf(isNetworkAvailable(context))
    }

    DisposableEffect(context, refreshKey) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (manager == null) {
            isOnline = false
            onDispose { }
        } else {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isOnline = isNetworkAvailable(context)
                }

                override fun onLost(network: Network) {
                    isOnline = isNetworkAvailable(context)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    isOnline = isNetworkAvailable(context)
                }
            }

            runCatching {
                manager.registerDefaultNetworkCallback(callback)
            }

            onDispose {
                runCatching {
                    manager.unregisterNetworkCallback(callback)
                }
            }
        }
    }

    return isOnline
}
