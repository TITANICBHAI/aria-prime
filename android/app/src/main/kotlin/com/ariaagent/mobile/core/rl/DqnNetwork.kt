package com.ariaagent.mobile.core.rl

import android.content.Context
import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Random

/**
 * DqnNetwork — Deep Q-Network for off-policy RL training.
 *
 * Implements DQN (Mnih et al., 2015) with the two key stabilisation tricks:
 *   1. Experience replay   — break temporal correlation by sampling random mini-batches
 *   2. Target network      — frozen copy of Q updated every TARGET_UPDATE_FREQ steps
 *
 * Architecture: identical MLP to PolicyNetwork (256 → 256 → 128 → 7)
 *   Input  : 256-dim (128 screen + 128 goal concatenation)
 *   Output : 7 Q-values, one per action
 *
 * Training (one step):
 *   1. Sample mini-batch of 32 transitions (s, a, r, s', done) from replay buffer
 *   2. Compute Bellman target: y = r  +  γ * max_{a'} Q_target(s', a') * (1 - done)
 *   3. MSE loss: L = mean[(Q(s, a) - y)²]
 *   4. Adam update on Q-network weights
 *   5. Every TARGET_UPDATE_FREQ steps: copy Q → Q_target (hard update)
 *
 * Exploration: ε-greedy
 *   ε decays linearly from 1.0 → EPS_MIN over EPS_DECAY_STEPS steps.
 *   selectAction() returns a random action with probability ε, greedy otherwise.
 *
 * Persistence: rl/dqn_online.bin + rl/dqn_target.bin (same format as PolicyNetwork)
 *
 * Usage in LearningScheduler:
 *   DqnNetwork.load(context)
 *   // during agent loop: DqnNetwork.selectAction(state)
 *   //                    DqnNetwork.storeTransition(s, a, r, s', done)
 *   // during training:   DqnNetwork.trainStep()   (call ~4× per agent step)
 *   DqnNetwork.save(context)
 */
object DqnNetwork {

    private const val TAG = "DqnNetwork"

    private const val INPUT_DIM  = 256
    private const val HIDDEN1    = 256
    private const val HIDDEN2    = 128
    private const val OUTPUT_DIM = 7

    /** Public aliases used by LearningScheduler and tests. */
    const val STATE_DIM = INPUT_DIM
    const val GOAL_DIM  = 0   // DQN uses a flat state vector (no separate goal embedding)

    private const val GAMMA              = 0.99f
    private const val LEARNING_RATE      = 1e-4f
    private const val EPS_START          = 1.0f
    private const val EPS_MIN            = 0.05f
    private const val EPS_DECAY_STEPS    = 50_000
    private const val BATCH_SIZE         = 32
    private const val REPLAY_CAPACITY    = 10_000
    private const val TARGET_UPDATE_FREQ = 500     // hard update every N train steps

    private const val BETA1     = 0.9f
    private const val BETA2     = 0.999f
    private const val ADAM_EPS  = 1e-8f

    private var isInitialized = false
    private var globalStep    = 0     // total environment steps (for ε schedule)
    private var trainStep     = 0     // total gradient steps (for target update)

    var lastQLoss: Double = 0.0
        private set

    val epsilon: Float
        get() {
            val frac = (globalStep.toFloat() / EPS_DECAY_STEPS).coerceIn(0f, 1f)
            return EPS_START + frac * (EPS_MIN - EPS_START)
        }

    // ─── Online network weights ────────────────────────────────────────────────
    private var w1:   FloatArray? = null   // HIDDEN1 × INPUT_DIM
    private var w2:   FloatArray? = null   // HIDDEN2 × HIDDEN1
    private var wo:   FloatArray? = null   // OUTPUT_DIM × HIDDEN2
    private var b1:   FloatArray? = null   // HIDDEN1
    private var b2:   FloatArray? = null   // HIDDEN2
    private var bo:   FloatArray? = null   // OUTPUT_DIM

    // ─── Adam state for online network ────────────────────────────────────────
    private var mW1: FloatArray? = null; private var vW1: FloatArray? = null
    private var mW2: FloatArray? = null; private var vW2: FloatArray? = null
    private var mWo: FloatArray? = null; private var vWo: FloatArray? = null
    private var mB1: FloatArray? = null; private var vB1: FloatArray? = null
    private var mB2: FloatArray? = null; private var vB2: FloatArray? = null
    private var mBo: FloatArray? = null; private var vBo: FloatArray? = null
    private var adamStep = 0

    // ─── Target network (frozen copy) ─────────────────────────────────────────
    private var tw1: FloatArray? = null
    private var tw2: FloatArray? = null
    private var two: FloatArray? = null
    private var tb1: FloatArray? = null
    private var tb2: FloatArray? = null
    private var tbo: FloatArray? = null

    // ─── Experience replay buffer ──────────────────────────────────────────────
    private data class Transition(
        val state:     FloatArray,
        val action:    Int,
        val reward:    Float,
        val nextState: FloatArray,
        val done:      Boolean
    )

    private val replayBuffer   = arrayOfNulls<Transition>(REPLAY_CAPACITY)
    private var replayHead     = 0
    private var replaySize     = 0
    private val rng            = Random(12345L)

    // ─── Load / save ──────────────────────────────────────────────────────────

    fun load(context: Context) {
        val rlDir = rlDir(context)
        val onlineFile = File(rlDir, "dqn_online.bin")
        if (onlineFile.exists() && onlineFile.length() > 100L) loadWeights(onlineFile, online = true)
        else initRandom()

        val targetFile = File(rlDir, "dqn_target.bin")
        if (targetFile.exists() && targetFile.length() > 100L) loadWeights(targetFile, online = false)
        else copyOnlineToTarget()

        initAdamState()
        isInitialized = true
        Log.i(TAG, "DqnNetwork loaded (epsilon=${epsilon}, replaySize=$replaySize)")
    }

    fun save(context: Context) {
        if (!isInitialized) return
        try {
            val dir = rlDir(context)
            writeWeights(File(dir, "dqn_online.bin"), online = true)
            writeWeights(File(dir, "dqn_target.bin"), online = false)
            Log.i(TAG, "DqnNetwork saved — trainStep=$trainStep, globalStep=$globalStep")
        } catch (e: Exception) {
            Log.e(TAG, "Save failed: ${e.message}")
        }
    }

    // ─── API ──────────────────────────────────────────────────────────────────

    /**
     * ε-greedy action selection.
     * Increments globalStep (used for ε decay).
     */
    fun selectAction(screenEmbedding: FloatArray, goalEmbedding: FloatArray): Pair<Int, Float> {
        if (!isInitialized) return Pair(0, 0f)
        globalStep++
        val input = concat(screenEmbedding, goalEmbedding)
        return if (rng.nextFloat() < epsilon) {
            Pair(rng.nextInt(OUTPUT_DIM), epsilon)
        } else {
            val q = forward(input, online = true)
            val best = q.indices.maxByOrNull { q[it] } ?: 0
            Pair(best, q[best])
        }
    }

    /**
     * Store one transition in the replay buffer.
     */
    fun storeTransition(
        screenEmb: FloatArray, goalEmb: FloatArray,
        action: Int, reward: Float,
        nextScreenEmb: FloatArray, nextGoalEmb: FloatArray,
        done: Boolean
    ) {
        val s  = concat(screenEmb, goalEmb)
        val ns = concat(nextScreenEmb, nextGoalEmb)
        replayBuffer[replayHead] = Transition(s, action.coerceIn(0, OUTPUT_DIM - 1), reward, ns, done)
        replayHead = (replayHead + 1) % REPLAY_CAPACITY
        if (replaySize < REPLAY_CAPACITY) replaySize++
    }

    /**
     * One gradient step. Noop if replay buffer has fewer than BATCH_SIZE entries.
     * Returns MSE loss, or 0.0 if skipped.
     */
    fun trainStep(): Double {
        if (!isInitialized || replaySize < BATCH_SIZE) return 0.0

        val batch = sampleBatch()
        val loss  = computeLossAndUpdate(batch)

        trainStep++
        if (trainStep % TARGET_UPDATE_FREQ == 0) {
            copyOnlineToTarget()
            Log.d(TAG, "Target network updated at trainStep=$trainStep")
        }

        lastQLoss = loss
        return loss
    }

    fun isReady(): Boolean = isInitialized

    // ─── Forward pass ─────────────────────────────────────────────────────────

    private fun forward(input: FloatArray, online: Boolean): FloatArray {
        val lw1 = if (online) w1!! else tw1!!
        val lw2 = if (online) w2!! else tw2!!
        val lwo = if (online) wo!! else two!!
        val lb1 = if (online) b1!! else tb1!!
        val lb2 = if (online) b2!! else tb2!!
        val lbo = if (online) bo!! else tbo!!

        val h1 = matVecRelu(lw1, lb1, input, HIDDEN1, INPUT_DIM)
        val h2 = matVecRelu(lw2, lb2, h1,    HIDDEN2, HIDDEN1)
        return matVecLinear(lwo, lbo, h2, OUTPUT_DIM, HIDDEN2)
    }

    private fun forwardWithActivations(input: FloatArray): Triple<FloatArray, FloatArray, FloatArray> {
        val h1 = matVecRelu(w1!!, b1!!, input, HIDDEN1, INPUT_DIM)
        val h2 = matVecRelu(w2!!, b2!!, h1,    HIDDEN2, HIDDEN1)
        val q  = matVecLinear(wo!!, bo!!, h2, OUTPUT_DIM, HIDDEN2)
        return Triple(q, h1, h2)
    }

    // ─── Bellman loss + Adam update ────────────────────────────────────────────

    private fun computeLossAndUpdate(batch: Array<Transition>): Double {
        val dW1 = FloatArray(HIDDEN1 * INPUT_DIM)
        val dW2 = FloatArray(HIDDEN2 * HIDDEN1)
        val dWo = FloatArray(OUTPUT_DIM * HIDDEN2)
        val dB1 = FloatArray(HIDDEN1)
        val dB2 = FloatArray(HIDDEN2)
        val dBo = FloatArray(OUTPUT_DIM)
        var totalLoss = 0.0

        for (t in batch) {
            // ── Bellman target ────────────────────────────────────────────────
            val qNext = forward(t.nextState, online = false)
            val maxQNext = if (t.done) 0f else qNext.max()
            val yTarget = t.reward + GAMMA * maxQNext

            // ── Online forward ────────────────────────────────────────────────
            val (q, h1, h2) = forwardWithActivations(t.state)

            // ── TD error on the taken action only ─────────────────────────────
            val tdErr = q[t.action] - yTarget
            totalLoss += tdErr.toDouble() * tdErr.toDouble()

            // ── Gradient at output layer (only the taken action gets gradient) ─
            val deltaOut = FloatArray(OUTPUT_DIM)
            deltaOut[t.action] = 2f * tdErr / BATCH_SIZE.toFloat()

            // ── Accumulate weight gradients ───────────────────────────────────
            for (i in 0 until OUTPUT_DIM) {
                dBo[i] += deltaOut[i]
                for (j in 0 until HIDDEN2) dWo[i * HIDDEN2 + j] += deltaOut[i] * h2[j]
            }

            val deltaH2 = FloatArray(HIDDEN2) { j ->
                var d = 0f
                for (i in 0 until OUTPUT_DIM) d += wo!![i * HIDDEN2 + j] * deltaOut[i]
                if (h2[j] > 0f) d else 0f
            }
            for (i in 0 until HIDDEN2) {
                dB2[i] += deltaH2[i]
                for (j in 0 until HIDDEN1) dW2[i * HIDDEN1 + j] += deltaH2[i] * h1[j]
            }

            val deltaH1 = FloatArray(HIDDEN1) { j ->
                var d = 0f
                for (i in 0 until HIDDEN2) d += w2!![i * HIDDEN1 + j] * deltaH2[i]
                if (h1[j] > 0f) d else 0f
            }
            for (i in 0 until HIDDEN1) {
                dB1[i] += deltaH1[i]
                for (j in 0 until INPUT_DIM) dW1[i * INPUT_DIM + j] += deltaH1[i] * t.state[j]
            }
        }

        // ── Adam update ───────────────────────────────────────────────────────
        adamStep++
        adamUpdate(w1!!,  dW1, mW1!!, vW1!!, adamStep)
        adamUpdate(w2!!,  dW2, mW2!!, vW2!!, adamStep)
        adamUpdate(wo!!,  dWo, mWo!!, vWo!!, adamStep)
        adamUpdate(b1!!,  dB1, mB1!!, vB1!!, adamStep)
        adamUpdate(b2!!,  dB2, mB2!!, vB2!!, adamStep)
        adamUpdate(bo!!,  dBo, mBo!!, vBo!!, adamStep)

        return totalLoss / BATCH_SIZE
    }

    private fun sampleBatch(): Array<Transition> =
        Array(BATCH_SIZE) { replayBuffer[rng.nextInt(replaySize)]!! }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun concat(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(INPUT_DIM)
        System.arraycopy(a, 0, out, 0,   minOf(a.size, 128))
        System.arraycopy(b, 0, out, 128, minOf(b.size, 128))
        return out
    }

    private fun matVecRelu(W: FloatArray, bias: FloatArray, x: FloatArray, rows: Int, cols: Int): FloatArray =
        FloatArray(rows) { i ->
            var s = bias[i]
            for (j in 0 until cols) s += W[i * cols + j] * x[j]
            if (s > 0f) s else 0f
        }

    private fun matVecLinear(W: FloatArray, bias: FloatArray, x: FloatArray, rows: Int, cols: Int): FloatArray =
        FloatArray(rows) { i ->
            var s = bias[i]
            for (j in 0 until cols) s += W[i * cols + j] * x[j]
            s
        }

    private fun adamUpdate(W: FloatArray, g: FloatArray, m: FloatArray, v: FloatArray, t: Int) {
        val bc1 = 1f - BETA1.pow(t)
        val bc2 = 1f - BETA2.pow(t)
        for (i in W.indices) {
            m[i] = BETA1 * m[i] + (1f - BETA1) * g[i]
            v[i] = BETA2 * v[i] + (1f - BETA2) * g[i] * g[i]
            W[i] -= LEARNING_RATE * (m[i] / bc1) / (kotlin.math.sqrt((v[i] / bc2).toDouble()).toFloat() + ADAM_EPS)
        }
    }

    private fun Float.pow(n: Int): Float { var r = 1f; repeat(n.coerceAtMost(200)) { r *= this }; return r }

    // ─── Init ─────────────────────────────────────────────────────────────────

    private fun initRandom() {
        val r = java.util.Random(99L)
        fun xavier(rows: Int, cols: Int) = FloatArray(rows * cols) {
            (r.nextGaussian() * kotlin.math.sqrt(2.0 / (rows + cols))).toFloat()
        }
        w1 = xavier(HIDDEN1, INPUT_DIM);   b1 = FloatArray(HIDDEN1)
        w2 = xavier(HIDDEN2, HIDDEN1);     b2 = FloatArray(HIDDEN2)
        wo = xavier(OUTPUT_DIM, HIDDEN2);  bo = FloatArray(OUTPUT_DIM)
        copyOnlineToTarget()
    }

    private fun copyOnlineToTarget() {
        tw1 = w1?.copyOf(); tw2 = w2?.copyOf(); two = wo?.copyOf()
        tb1 = b1?.copyOf(); tb2 = b2?.copyOf(); tbo = bo?.copyOf()
    }

    private fun initAdamState() {
        mW1 = FloatArray(HIDDEN1 * INPUT_DIM);  vW1 = FloatArray(HIDDEN1 * INPUT_DIM)
        mW2 = FloatArray(HIDDEN2 * HIDDEN1);    vW2 = FloatArray(HIDDEN2 * HIDDEN1)
        mWo = FloatArray(OUTPUT_DIM * HIDDEN2); vWo = FloatArray(OUTPUT_DIM * HIDDEN2)
        mB1 = FloatArray(HIDDEN1);              vB1 = FloatArray(HIDDEN1)
        mB2 = FloatArray(HIDDEN2);              vB2 = FloatArray(HIDDEN2)
        mBo = FloatArray(OUTPUT_DIM);           vBo = FloatArray(OUTPUT_DIM)
        adamStep = 0
    }

    // ─── Binary persistence ────────────────────────────────────────────────────

    private fun writeWeights(file: File, online: Boolean) {
        val lw1 = if (online) w1!! else tw1!!
        val lw2 = if (online) w2!! else tw2!!
        val lwo = if (online) wo!! else two!!
        val lb1 = if (online) b1!! else tb1!!
        val lb2 = if (online) b2!! else tb2!!
        val lbo = if (online) bo!! else tbo!!
        DataOutputStream(FileOutputStream(file)).use { out ->
            out.writeInt(globalStep); out.writeInt(trainStep); out.writeInt(adamStep)
            for (arr in listOf(lw1, lw2, lwo, lb1, lb2, lbo)) {
                out.writeInt(arr.size); arr.forEach { out.writeFloat(it) }
            }
        }
    }

    private fun loadWeights(file: File, online: Boolean) {
        try {
            DataInputStream(FileInputStream(file)).use { din ->
                val gs = din.readInt(); val ts = din.readInt(); val as_ = din.readInt()
                if (online) { globalStep = gs; trainStep = ts; adamStep = as_ }
                val arrays = (1..6).map {
                    val sz = din.readInt()
                    FloatArray(sz) { din.readFloat() }
                }
                if (online) {
                    w1 = arrays[0]; w2 = arrays[1]; wo = arrays[2]
                    b1 = arrays[3]; b2 = arrays[4]; bo = arrays[5]
                } else {
                    tw1 = arrays[0]; tw2 = arrays[1]; two = arrays[2]
                    tb1 = arrays[3]; tb2 = arrays[4]; tbo = arrays[5]
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Load failed (${if (online) "online" else "target"}): ${e.message}")
            if (online) initRandom() else copyOnlineToTarget()
        }
    }

    private fun rlDir(context: Context): File =
        File(context.filesDir, "rl").also { it.mkdirs() }
            .let { if (it.canWrite()) it else (context.getExternalFilesDir("rl") ?: it).also { d -> d.mkdirs() } }
}
