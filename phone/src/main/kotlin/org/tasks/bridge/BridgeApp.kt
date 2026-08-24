package org.tasks.bridge

import android.app.Application
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/**
 * Registers the phone capability (`org.tasks.phone.bridge`) on startup so the
 * wear side can discover this device via CapabilityClient.
 */
class BridgeApp : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            try {
                Wearable.getCapabilityClient(this@BridgeApp)
                    .addLocalCapability(BridgeApp.CAPABILITY).await()
            } catch (_: Exception) {
                // Play services unavailable; the bridge will retry next launch.
            }
        }
    }

    companion object {
        const val CAPABILITY = "org.tasks.phone.bridge"
    }
}
