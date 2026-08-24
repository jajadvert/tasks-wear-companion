package org.tasks.bridge

import android.app.Application
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable

/**
 * Registers the phone capability (`org.tasks.phone.bridge`) on startup so the
 * wear side can discover this device via CapabilityClient. Fire-and-forget:
 * failures are ignored (the bridge retries on next app start).
 */
class BridgeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Wearable.getCapabilityClient(this)
            .addLocalCapability(CAPABILITY)
    }

    companion object {
        const val CAPABILITY = "org.tasks.phone.bridge"
    }
}
