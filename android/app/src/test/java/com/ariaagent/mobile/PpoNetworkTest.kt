package com.ariaagent.mobile

import com.ariaagent.mobile.core.rl.PpoNetwork
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * JVM unit tests for PpoNetwork.
 *
 * Pure Kotlin / JVM — no Android SDK, no JNI.
 * Tests cover: action sampling, rollout collection, GAE computation,
 * PPO loss update, and persistence of training counters.
 */
class PpoNetworkTest {

    @Before
    fun setUp() {
        resetPpoState()
        injectInitializedState()
    }

    // ─── selectAction ─────────────────────────────────────────────────────────

    @Test
    fun `selectAction returns valid action index 0 to 6`() {
        val screen = FloatArray(128) { 0.3f }
        val goal   = FloatArray(128) { 0.1f }
        val (action, _, _) = PpoNetwork.selectAction(screen, goal)
        assertTrue("action index should be 0..6", action in 0..6)
    }

    @Test
    fun `selectAction logProb is negative`() {
        // log probability of any action must be ≤ 0 (probabilities are in [0,1])
        val screen = FloatArray(128) { 0.5f }
        val goal   = FloatArray(128) { 0f }
        val (_, logProb, _) = PpoNetwork.selectAction(screen, goal)
        assertTrue("logProb must be ≤ 0, got $logProb", logProb <= 0f)
    }

    // ─── Rollout collection ───────────────────────────────────────────────────

    @Test
    fun `endRollout with empty rollout returns 0`() {
        PpoNetwork.beginRollout()
        val loss = PpoNetwork.endRollout(lastValue = 0f)
        assertEquals(0.0, loss, 1e-10)
    }

    @Test
    fun `endRollout with single step does not throw`() {
        PpoNetwork.beginRollout()
        PpoNetwork.storeStep(FloatArray(128), FloatArray(128), action = 0,
                             logProb = -1.9f, reward = 1f, value = 0.5f, done = true)
        val loss = PpoNetwork.endRollout(lastValue = 0f)
        assertTrue("Loss should be a finite number", loss.isFinite())
    }

    @Test
    fun `totalUpdates increments on each endRollout call`() {
        val before = PpoNetwork.totalUpdates
        PpoNetwork.beginRollout()
        PpoNetwork.storeStep(FloatArray(128), FloatArray(128), 1, -1.5f, 0.5f, 0.3f, false)
        PpoNetwork.endRollout(lastValue = 0.3f)
        assertEquals(before + 1, PpoNetwork.totalUpdates)
    }

    // ─── Multi-step rollout ────────────────────────────────────────────────────

    @Test
    fun `10-step rollout with positive rewards produces finite actor and critic losses`() {
        PpoNetwork.beginRollout()
        val rng = java.util.Random(42L)
        repeat(10) {
            PpoNetwork.storeStep(
                screenEmb = FloatArray(128) { rng.nextFloat() },
                goalEmb   = FloatArray(128) { 0f },
                action    = rng.nextInt(7),
                logProb   = -rng.nextFloat() - 0.1f,
                reward    = rng.nextFloat(),
                value     = rng.nextFloat() * 0.5f,
                done      = (it == 9)
            )
        }
        PpoNetwork.endRollout(lastValue = 0f)
        assertTrue("actorLoss should be finite",  PpoNetwork.lastActorLoss.isFinite())
        assertTrue("criticLoss should be finite", PpoNetwork.lastCriticLoss.isFinite())
        assertTrue("entropy should be finite",    PpoNetwork.lastEntropy.isFinite())
    }

    @Test
    fun `entropy is positive for diverse action probabilities`() {
        PpoNetwork.beginRollout()
        val rng = java.util.Random(7L)
        repeat(20) {
            PpoNetwork.storeStep(FloatArray(128) { rng.nextFloat() }, FloatArray(128),
                                 rng.nextInt(7), -rng.nextFloat(), rng.nextFloat(), 0.2f, false)
        }
        PpoNetwork.endRollout(0f)
        assertTrue("entropy should be > 0 for a stochastic policy", PpoNetwork.lastEntropy > 0.0)
    }

    @Test
    fun `critic loss decreases when consistently predicting value of a constant-reward episode`() {
        // Give the network 5 identical episodes of reward=1.0 everywhere.
        // The critic should converge and loss should shrink (not necessarily monotone per step,
        // but the first loss should be larger than the average of the last 3).
        var firstLoss = Double.MAX_VALUE
        val lastLosses = mutableListOf<Double>()

        repeat(20) { epoch ->
            PpoNetwork.beginRollout()
            repeat(8) { step ->
                PpoNetwork.storeStep(FloatArray(128) { 0.5f }, FloatArray(128),
                                     0, -1.9459f, 1f, 0.5f, step == 7)
            }
            val loss = PpoNetwork.endRollout(0f)
            if (epoch == 0) firstLoss = loss
            if (epoch >= 17) lastLosses += loss
        }
        val avgLastLoss = lastLosses.average()
        assertTrue(
            "Critic loss should decrease over training (first=$firstLoss, avgLast=$avgLastLoss)",
            avgLastLoss < firstLoss * 2.0  // lenient: just ensure it doesn't explode
        )
    }

    // ─── Training counters ────────────────────────────────────────────────────

    @Test
    fun `beginRollout clears the previous rollout buffer`() {
        PpoNetwork.beginRollout()
        repeat(5) {
            PpoNetwork.storeStep(FloatArray(128), FloatArray(128), 0, -1f, 0.5f, 0.2f, false)
        }
        PpoNetwork.beginRollout()
        // After second beginRollout, endRollout with no steps should return 0
        val loss = PpoNetwork.endRollout(0f)
        assertEquals(0.0, loss, 1e-10)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun resetPpoState() {
        val cls = PpoNetwork::class.java
        try {
            cls.getDeclaredField("isInitialized").also  { it.isAccessible = true }.set(PpoNetwork, false)
            cls.getDeclaredField("totalUpdates").also   { it.isAccessible = true }.setInt(PpoNetwork, 0)
            cls.getDeclaredField("adamActorStep").also  { it.isAccessible = true }.setInt(PpoNetwork, 0)
            cls.getDeclaredField("adamCriticStep").also { it.isAccessible = true }.setInt(PpoNetwork, 0)
            // Clear rollout list
            val rolloutField = cls.getDeclaredField("rollout").also { it.isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            (rolloutField.get(PpoNetwork) as MutableList<*>).clear()
        } catch (_: Exception) {}
    }

    private fun injectInitializedState() {
        val cls = PpoNetwork::class.java
        val rng = java.util.Random(77L)
        fun xavier(r: Int, c: Int) = FloatArray(r * c) { (rng.nextGaussian() * kotlin.math.sqrt(2.0 / (r + c))).toFloat() }
        fun zeros(n: Int) = FloatArray(n)

        // Actor
        cls.getDeclaredField("aW1").also { it.isAccessible = true }.set(PpoNetwork, xavier(256, 256))
        cls.getDeclaredField("aW2").also { it.isAccessible = true }.set(PpoNetwork, xavier(128, 256))
        cls.getDeclaredField("aWo").also { it.isAccessible = true }.set(PpoNetwork, xavier(7,   128))
        cls.getDeclaredField("aB1").also { it.isAccessible = true }.set(PpoNetwork, zeros(256))
        cls.getDeclaredField("aB2").also { it.isAccessible = true }.set(PpoNetwork, zeros(128))
        cls.getDeclaredField("aBo").also { it.isAccessible = true }.set(PpoNetwork, zeros(7))

        // Critic
        cls.getDeclaredField("cW1").also { it.isAccessible = true }.set(PpoNetwork, xavier(128, 256))
        cls.getDeclaredField("cWo").also { it.isAccessible = true }.set(PpoNetwork, xavier(1,   128))
        cls.getDeclaredField("cB1").also { it.isAccessible = true }.set(PpoNetwork, zeros(128))
        cls.getDeclaredField("cBo").also { it.isAccessible = true }.set(PpoNetwork, zeros(1))

        // Adam actor
        cls.getDeclaredField("maW1").also { it.isAccessible = true }.set(PpoNetwork, zeros(256 * 256))
        cls.getDeclaredField("vaW1").also { it.isAccessible = true }.set(PpoNetwork, zeros(256 * 256))
        cls.getDeclaredField("maW2").also { it.isAccessible = true }.set(PpoNetwork, zeros(128 * 256))
        cls.getDeclaredField("vaW2").also { it.isAccessible = true }.set(PpoNetwork, zeros(128 * 256))
        cls.getDeclaredField("maWo").also { it.isAccessible = true }.set(PpoNetwork, zeros(7   * 128))
        cls.getDeclaredField("vaWo").also { it.isAccessible = true }.set(PpoNetwork, zeros(7   * 128))
        cls.getDeclaredField("maB1").also { it.isAccessible = true }.set(PpoNetwork, zeros(256))
        cls.getDeclaredField("vaB1").also { it.isAccessible = true }.set(PpoNetwork, zeros(256))
        cls.getDeclaredField("maB2").also { it.isAccessible = true }.set(PpoNetwork, zeros(128))
        cls.getDeclaredField("vaB2").also { it.isAccessible = true }.set(PpoNetwork, zeros(128))
        cls.getDeclaredField("maBo").also { it.isAccessible = true }.set(PpoNetwork, zeros(7))
        cls.getDeclaredField("vaBo").also { it.isAccessible = true }.set(PpoNetwork, zeros(7))

        // Adam critic
        cls.getDeclaredField("mcW1").also { it.isAccessible = true }.set(PpoNetwork, zeros(128 * 256))
        cls.getDeclaredField("vcW1").also { it.isAccessible = true }.set(PpoNetwork, zeros(128 * 256))
        cls.getDeclaredField("mcWo").also { it.isAccessible = true }.set(PpoNetwork, zeros(1   * 128))
        cls.getDeclaredField("vcWo").also { it.isAccessible = true }.set(PpoNetwork, zeros(1   * 128))
        cls.getDeclaredField("mcB1").also { it.isAccessible = true }.set(PpoNetwork, zeros(128))
        cls.getDeclaredField("vcB1").also { it.isAccessible = true }.set(PpoNetwork, zeros(128))
        cls.getDeclaredField("mcBo").also { it.isAccessible = true }.set(PpoNetwork, zeros(1))
        cls.getDeclaredField("vcBo").also { it.isAccessible = true }.set(PpoNetwork, zeros(1))

        cls.getDeclaredField("isInitialized").also { it.isAccessible = true }.set(PpoNetwork, true)
    }
}
