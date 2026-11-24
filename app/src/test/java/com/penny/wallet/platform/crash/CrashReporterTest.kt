package com.penny.wallet.platform.crash

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReporterTest {
    @Test fun reportHasClassesAndFramesButNoMessages() {
        val e = IllegalStateException("note: Rent 950,00", RuntimeException("secret 1234"))
        val r = CrashReporter.format(e, "1.6", "2026-11-24T20:00:00Z", device = "Xiaomi 23108RN04Y, Android 15 (API 35)")
        assertTrue(r.contains("Penny Wallet 1.6"))
        assertTrue(r.contains("java.lang.IllegalStateException"))
        assertTrue(r.contains("Caused by: java.lang.RuntimeException"))
        assertTrue(r.contains("    at "))
        assertFalse(r.contains("Rent"))
        assertFalse(r.contains("secret"))
    }

    @Test fun reportIsBounded() {
        fun deep(n: Int): Throwable = if (n == 0) Error() else RuntimeException(deep(n - 1))
        assertTrue(CrashReporter.format(deep(50), "1", "t", device = "d").length <= 6_000)
    }
}
