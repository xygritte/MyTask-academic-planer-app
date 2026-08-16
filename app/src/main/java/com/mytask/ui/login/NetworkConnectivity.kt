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
import com.google.firebase.auth.FirebaseAuth

fun isNetworkAvailable(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false

    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

/**
 * Workspace availability for the app shell.
 * An already authenticated Firebase session may continue using the local Room
 * workspace while the device is offline. Call [isNetworkAvailable] directly
 * for operations that actually require an internet connection.
 */
@Composable
fun rememberNetworkAvailable(
    context: Context,
    refreshKey: Int = 0
): Boolean {
    var actualNetworkAvailable by remember(context, refreshKey) {
        mutableStateOf(isNetworkAvailable(context))
    }

    DisposableEffect(context, refreshKey) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (manager == null) {
            actualNetworkAvailable = false
            onDispose { }
        } else {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    actualNetworkAvailable = isNetworkAvailable(context)
                }

                override fun onLost(network: Network) {
                    actualNetworkAvailable = isNetworkAvailable(context)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    actualNetworkAvailable = isNetworkAvailable(context)
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

    val authenticated = FirebaseAuth.getInstance().currentUser != null
    return authenticated || actualNetworkAvailable
}
