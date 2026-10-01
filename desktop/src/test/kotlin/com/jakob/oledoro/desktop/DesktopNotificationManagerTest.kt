package com.jakob.oledoro.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopNotificationManagerTest {

    @Test
    fun `initialize requires coroutineScope and is idempotent`() = runTest {
        val testScope = CoroutineScope(Dispatchers.IO)
        DesktopNotificationManager.initialize(testScope)
        DesktopNotificationManager.initialize(testScope) // Should not throw
        DesktopNotificationManager.cleanup()
    }

    @Test
    fun `cleanup resets initialized flag`() = runTest {
        val testScope = CoroutineScope(Dispatchers.IO)
        DesktopNotificationManager.initialize(testScope)
        DesktopNotificationManager.cleanup()
        // After cleanup, initialize should work again
        DesktopNotificationManager.initialize(testScope)
        DesktopNotificationManager.cleanup()
    }

    @Test
    fun `notifyTimerFinished does not throw`() = runTest {
        val testScope = CoroutineScope(Dispatchers.IO)
        DesktopNotificationManager.initialize(testScope)
        DesktopNotificationManager.notifyTimerFinished("Focus", "Break")
        DesktopNotificationManager.cleanup()
    }

    @Test
    fun `notifyPhaseStarted does not throw`() = runTest {
        val testScope = CoroutineScope(Dispatchers.IO)
        DesktopNotificationManager.initialize(testScope)
        DesktopNotificationManager.notifyPhaseStarted("Focus")
        DesktopNotificationManager.cleanup()
    }

    @Test
    fun `multiple notifications work`() = runTest {
        val testScope = CoroutineScope(Dispatchers.IO)
        DesktopNotificationManager.initialize(testScope)
        DesktopNotificationManager.notifyTimerFinished("Focus", "Short Break")
        DesktopNotificationManager.notifyPhaseStarted("Short Break")
        DesktopNotificationManager.notifyTimerFinished("Short Break", "Focus")
        DesktopNotificationManager.cleanup()
    }
}