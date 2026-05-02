package com.ariaagent.mobile

import com.ariaagent.mobile.ui.viewmodel.SafetyConfig
import org.junit.Assert.*
import org.junit.Test

class SafetyConfigTest {

    @Test
    fun `default config has empty sets and false flags`() {
        val config = SafetyConfig()
        assertFalse(config.globalKillActive)
        assertFalse(config.confirmMode)
        assertFalse(config.allowlistMode)
        assertTrue(config.blockedPackages.isEmpty())
        assertTrue(config.allowedPackages.isEmpty())
        assertTrue(config.customSensitivePackages.isEmpty())
    }

    @Test
    fun `adding a blocked package produces new set without mutation`() {
        val original = SafetyConfig()
        val updated  = original.copy(blockedPackages = original.blockedPackages + "com.example.app")
        assertTrue("com.example.app" in updated.blockedPackages)
        assertTrue(original.blockedPackages.isEmpty()) // original is unchanged
    }

    @Test
    fun `customSensitivePackages is independent from blockedPackages`() {
        val config = SafetyConfig(
            blockedPackages          = setOf("com.blocked.app"),
            customSensitivePackages  = setOf("com.sensitive.app")
        )
        assertFalse("com.sensitive.app" in config.blockedPackages)
        assertFalse("com.blocked.app"   in config.customSensitivePackages)
    }

    @Test
    fun `removing a package from customSensitivePackages leaves others intact`() {
        val config  = SafetyConfig(customSensitivePackages = setOf("com.a", "com.b", "com.c"))
        val updated = config.copy(customSensitivePackages = config.customSensitivePackages - "com.b")
        assertEquals(setOf("com.a", "com.c"), updated.customSensitivePackages)
    }

    @Test
    fun `allowlistMode with empty allowedPackages allows nothing`() {
        val config = SafetyConfig(allowlistMode = true, allowedPackages = emptySet())
        assertTrue(config.allowlistMode)
        assertTrue(config.allowedPackages.isEmpty())
    }

    @Test
    fun `globalKillActive and confirmMode can be toggled independently`() {
        val config = SafetyConfig(globalKillActive = true, confirmMode = false)
        val toggled = config.copy(globalKillActive = false, confirmMode = true)
        assertFalse(toggled.globalKillActive)
        assertTrue(toggled.confirmMode)
    }
}
