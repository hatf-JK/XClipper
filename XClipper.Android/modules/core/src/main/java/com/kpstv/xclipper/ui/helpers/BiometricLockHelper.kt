package com.kpstv.xclipper.ui.helpers

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.kpstv.xclipper.ui.activities.Start

class BiometricLockHelper(
    private val activity: Start,
    private val appSettings: AppSettings
) : DefaultLifecycleObserver {

    private var lastUnlockTime: Long = -1L
    private val LOCK_TIMEOUT = 60 * 1000L // 1 minute

    fun register() {
        activity.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (appSettings.isAppLockEnabled()) {
            if (lastUnlockTime == -1L || System.currentTimeMillis() - lastUnlockTime > LOCK_TIMEOUT) {
                authenticate()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (appSettings.isAppLockEnabled() && lastUnlockTime != -1L) {
             // We don't reset lastUnlockTime here, we rely on timeout.
             // If user leaves and comes back quickly, no lock.
        }
    }

    private fun authenticate() {
        if (AppSecurityHelper.isAuthenticationAvailable(activity)) {
            AppSecurityHelper.authenticate(
                activity = activity,
                onSuccess = {
                    lastUnlockTime = System.currentTimeMillis()
                },
                onFailure = {
                    activity.finishAffinity()
                }
            )
        }
    }
}
