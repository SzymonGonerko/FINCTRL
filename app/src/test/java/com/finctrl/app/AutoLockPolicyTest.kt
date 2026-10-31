package com.finctrl.app

import com.finctrl.app.security.AutoLockPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLockPolicyTest {
    private val policy = AutoLockPolicy(timeoutMs = 30_000)

    @Test fun `first start without background does not lock`() =
        assertFalse(policy.onForeground(nowMs = 1_000))

    @Test fun `short trip to background does not lock`() {
        policy.onBackground(nowMs = 10_000)
        assertFalse(policy.onForeground(nowMs = 25_000))
    }

    @Test fun `background longer than timeout locks`() {
        policy.onBackground(nowMs = 10_000)
        assertTrue(policy.onForeground(nowMs = 40_000))
    }

    @Test fun `each background period is evaluated separately`() {
        policy.onBackground(nowMs = 0)
        assertTrue(policy.onForeground(nowMs = 60_000))
        assertFalse(policy.onForeground(nowMs = 200_000))
    }
}
