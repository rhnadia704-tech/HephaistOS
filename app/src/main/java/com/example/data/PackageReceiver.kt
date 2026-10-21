package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PackageReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PackageReceiver"
        private val listeners = mutableListOf<(String, String) -> Unit>()

        fun addListener(listener: (packageName: String, action: String) -> Unit) {
            synchronized(listeners) {
                if (!listeners.contains(listener)) {
                    listeners.add(listener)
                }
            }
        }

        fun removeListener(listener: (packageName: String, action: String) -> Unit) {
            synchronized(listeners) {
                listeners.remove(listener)
            }
        }

        private fun notifyListeners(packageName: String, action: String) {
            synchronized(listeners) {
                for (listener in listeners) {
                    try {
                        listener(packageName, action)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error notifying package listener", e)
                    }
                }
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        val action = intent.action ?: return
        val data = intent.data
        val packageName = data?.schemeSpecificPart ?: return

        Log.d(TAG, "Événement de paquet détecté : action=$action, package=$packageName")

        when (action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                notifyListeners(packageName, action)
            }
        }
    }
}
