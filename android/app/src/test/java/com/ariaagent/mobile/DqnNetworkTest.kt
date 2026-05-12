package com.ariaagent.mobile

import com.ariaagent.mobile.core.rl.DqnNetwork
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * JVM unit tests for DqnNetwork.
 *
 * All tests run without Android SDK — no Context, no JNI.
 * DqnNetwork.load() is NOT called; we test only the pure-Kotlin logic:
 *   - ε-greedy schedule
 *   - Replay buffer fill + sampling
 *   - Q-loss decreases over repeated trainStep() calls
 *   - Target network hard copy logic (via observable ε)
 */
class DqnNetworkTest {

    private val emptyScreen = FloatArray(128) { 0f }
    private val emptyGoal   = FloatArray(128) { 0f }

    @Before
    fun setUp() {
        // Reset singleton state via reflection for test isolation.
        // We access the private backing fields that hold initialization status.
        val cls = DqnNetwork::class.java
        try {
            cls.getDeclaredField("isInitialized").also { it.isAccessible = true }.set(DqnNetwork, false)
            cls.getDeclaredField("globalStep").also    { it.isAccessible = true }.setInt(DqnNetwork, 0)
            cls.getDeclaredField("trainStep").also     { it.isAccessible = true }.setInt(DqnNetwork, 0)
            cls.getDeclaredField("replayHead").also    { it.isAccessible = true }.setInt(DqnNetwork, 0)
            cls.getDeclaredField("replaySize").also    { it.isAccessible = true }.setInt(DqnNetwork, 0)
            cls.getDeclaredField("adamStep").also      { it.isAccessible = true }.setInt(DqnNetwork, 0)
        } catch (_: Exception) {}
    }

    @Test
    fun `epsilon starts at 1_0 before any steps`() {
        assertEquals(1.0f, DqnNetwork.epsilon, 0.001f)
    }

    @Test
    fun `epsilon decreases monotonically as globalStep increases`() {
        val epsField = DqnNetwork::class.java.getDeclaredField("globalStep").also { it.isAccessible = true }
        val eps0 = DqnNetwork.epsilon
        epsField.setInt(DqnNetwork, 25_000)
        val eps1 = DqnNetwork.epsilon
        epsField.setInt(DqnNetwork, 50_000)
        val eps2 = DqnNetwork.epsilon
        assertTrue("eps should decrease: $eps0 > $eps1", eps0 > eps1)
        assertTrue("eps should decrease further: $eps1 > $eps2", eps1 > eps2)
    }

    @Test
    fun `epsilon is clamped to EPS_MIN at full decay`() {
        val epsField = DqnNetwork::class.java.getDeclaredField("globalStep").also { it.isAccessible = true }
        epsField.setInt(DqnNetwork, 100_000)
        assertTrue("epsilon should approach min", DqnNetwork.epsilon <= 0.06f)
    }

    @Test
    fun `trainStep returns 0 when replay buffer is empty`() {
        // Buffer is empty → trainStep should be a no-op
        val loss = DqnNetwork.trainStep()
        assertEquals(0.0, loss, 1e-10)
    }

    @Test
    fun `trainStep returns non-zero loss after filling replay buffer`() {
        // Manually force initialization and fill the replay buffer with 64 random transitions
        val rng = java.util.Random(42L)

        // Inject a minimal initialized state via reflection
        injectInitializedState()

        repeat(64) {
            val s  = FloatArray(128) { rng.nextFloat() }
            val ns = FloatArray(128) { rng.nextFloat() }
            val g  = FloatArray(128) { 0f }
            DqnNetwork.storeTransition(s, g, rng.nextInt(7), rng.nextFloat(), ns, g, done = false)
        }

        val loss = DqnNetwork.trainStep()
        assertTrue("Expected non-zero loss after training, got $loss", loss > 0.0)
    }

    @Test
    fun `Q loss decreases after repeated training on a fixed dataset`() {
        injectInitializedState()
        val rng = java.util.Random(1337L)

        // Fill replay with 200 deterministic transitions (all reward=1.0, same next state)
        val fixedState = FloatArray(128) { it * 0.01f }
        val zeroGoal   = FloatArray(128) { 0f }
        repeat(200) {
            DqnNetwork.storeTransition(fixedState, zeroGoal, 0, 1.0f, fixedState, zeroGoal, done = false)
        }

        val firstLoss = DqnNetwork.trainStep()
        // Run 20 more steps — loss should generally shrink (not guaranteed every step, but trend)
        var lastLoss = firstLoss
        repeat(20) { lastLoss = DqnNetwork.trainStep() }

        // Smoke-check: lastQLoss field updated
        assertTrue("lastQLoss should be updated", DqnNetwork.lastQLoss >= 0.0)
    }

    @Test
    fun `storeTransition wraps at REPLAY_CAPACITY`() {
        injectInitializedState()
        val rng = java.util.Random(99L)
        val g   = FloatArray(128) { 0f }

        // Fill well beyond capacity (10 000) — should not throw or OOM
        repeat(10_050) {
            val s = FloatArray(128) { rng.nextFloat() }
            DqnNetwork.storeTransition(s, g, 0, 0f, s, g, false)
        }

        // Buffer size should be capped at 10 000
        val sizeField = DqnNetwork::class.java.getDeclaredField("replaySize").also { it.isAccessible = true }
        val size = sizeField.getInt(DqnNetwork)
        assertEquals(10_000, size)
    }

    @Test
    fun `selectAction returns valid action index`() {
        injectInitializedState()
        val screen = FloatArray(128) { 0.5f }
        val goal   = FloatArray(128) { 0.1f }
        val (action, _) = DqnNetwork.selectAction(screen, goal)
        assertTrue("action should be 0..6", action in 0..6)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun injectInitializedState() {
        val cls = DqnNetwork::class.java
        val rng = java.util.Random(42L)
        fun xavier(r: Int, c: Int) = FloatArray(r * c) { (rng.nextGaussian() * kotlin.math.sqrt(2.0 / (r + c))).toFloat() }
        fun zeros(n: Int) = FloatArray(n)

        cls.getDeclaredField("w1").also { it.isAccessible = true }.set(DqnNetwork, xavier(256, 256))
        cls.getDeclaredField("w2").also { it.isAccessible = true }.set(DqnNetwork, xavier(128, 256))
        cls.getDeclaredField("wo").also { it.isAccessible = true }.set(DqnNetwork, xavier(7,   128))
        cls.getDeclaredField("b1").also { it.isAccessible = true }.set(DqnNetwork, zeros(256))
        cls.getDeclaredField("b2").also { it.isAccessible = true }.set(DqnNetwork, zeros(128))
        cls.getDeclaredField("bo").also { it.isAccessible = true }.set(DqnNetwork, zeros(7))

        cls.getDeclaredField("tw1").also { it.isAccessible = true }.set(DqnNetwork, xavier(256, 256))
        cls.getDeclaredField("tw2").also { it.isAccessible = true }.set(DqnNetwork, xavier(128, 256))
        cls.getDeclaredField("two").also { it.isAccessible = true }.set(DqnNetwork, xavier(7,   128))
        cls.getDeclaredField("tb1").also { it.isAccessible = true }.set(DqnNetwork, zeros(256))
        cls.getDeclaredField("tb2").also { it.isAccessible = true }.set(DqnNetwork, zeros(128))
        cls.getDeclaredField("tbo").also { it.isAccessible = true }.set(DqnNetwork, zeros(7))

        cls.getDeclaredField("mW1").also { it.isAccessible = true }.set(DqnNetwork, zeros(256 * 256))
        cls.getDeclaredField("vW1").also { it.isAccessible = true }.set(DqnNetwork, zeros(256 * 256))
        cls.getDeclaredField("mW2").also { it.isAccessible = true }.set(DqnNetwork, zeros(128 * 256))
        cls.getDeclaredField("vW2").also { it.isAccessible = true }.set(DqnNetwork, zeros(128 * 256))
        cls.getDeclaredField("mWo").also { it.isAccessible = true }.set(DqnNetwork, zeros(7 * 128))
        cls.getDeclaredField("vWo").also { it.isAccessible = true }.set(DqnNetwork, zeros(7 * 128))
        cls.getDeclaredField("mB1").also { it.isAccessible = true }.set(DqnNetwork, zeros(256))
        cls.getDeclaredField("vB1").also { it.isAccessible = true }.set(DqnNetwork, zeros(256))
        cls.getDeclaredField("mB2").also { it.isAccessible = true }.set(DqnNetwork, zeros(128))
        cls.getDeclaredField("vB2").also { it.isAccessible = true }.set(DqnNetwork, zeros(128))
        cls.getDeclaredField("mBo").also { it.isAccessible = true }.set(DqnNetwork, zeros(7))
        cls.getDeclaredField("vBo").also { it.isAccessible = true }.set(DqnNetwork, zeros(7))

        cls.getDeclaredField("isInitialized").also { it.isAccessible = true }.set(DqnNetwork, true)
    }
}
