package com.ariaagent.mobile.core.rl

import android.content.Context
import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * PpoNetwork — Proximal Policy Optimization (PPO-Clip) for on-policy RL.
 *
 * Implements PPO (Schulman et al., 2017) with:
 *   - Actor network  : MLP 256 → 256 → 128 → 7 (softmax action probabilities)
 *   - Critic network : MLP 256 → 128 → 1  (state value estimate)
 *   - GAE            : Generalized Advantage Estimation (λ=0.95) for variance reduction
 *   - PPO-Clip       : L_CLIP = E[min(r_t * A_t,  clip(r_t, 1-ε, 1+ε) * A_t)]
 *   - Value loss     : L_V   = (V(s) - R_t)²
 *   - Entropy bonus  : H    = -Σ π_i * log(π_i)
 *   - Total loss     : L = -L_CLIP + c_v * L_V - c_e * H
 *
 * Usage pattern:
 *   1. beginRollout()
 *   2. Per step: selectAction(s) → (action, logProb, value); storeStep(s, a, logProb, r, v, done)
 *   3. endRollout(lastValue): calls computeGAE + trains K epochs
 *   4. save(context)
 *
 * Both networks are saved / loaded independently (ppo_actor.bin, ppo_critic.bin).
 * Persistence format: little-endian float32 arrays, same layout as PolicyNetwork.
 */
object PpoNetwork {

    private const val TAG = "PpoNetwork"

    // ── Architecture ──────────────────────────────────────────────────────────
    private const val INPUT_DIM     = 256
    private const val ACT_H1        = 256
    private const val ACT_H2        = 128
    private const val ACT_OUT       = 7       // action space
    private const val CRIT_H1       = 128
    private const val CRIT_OUT      = 1

    // ── PPO hyperparameters ───────────────────────────────────────────────────
    private const val GAMMA         = 0.99f
    private const val LAMBDA        = 0.95f   // GAE parameter
    private const val CLIP_EPS      = 0.2f
    private const val ENTROPY_COEFF = 0.01f
    private const val VALUE_COEFF   = 0.5f
    private const val LR_ACTOR      = 3e-4f
    private const val LR_CRITIC     = 1e-3f
    private const val K_EPOCHS      = 3
    private const val MAX_GRAD_NORM = 0.5f    // gradient clipping

    // ── Adam ──────────────────────────────────────────────────────────────────
    private const val BETA1    = 0.9f
    private const val BETA2    = 0.999f
    private const val ADAM_EPS = 1e-8f

    var isInitialized = false
        private set

    var lastActorLoss:  Double = 0.0; private set
    var lastCriticLoss: Double = 0.0; private set
    var lastEntropy:    Double = 0.0; private set
    var totalUpdates:   Int    = 0;   private set

    // ── Actor weights ─────────────────────────────────────────────────────────
    private var aW1: FloatArray? = null   // ACT_H1 × INPUT_DIM
    private var aW2: FloatArray? = null   // ACT_H2 × ACT_H1
    private var aWo: FloatArray? = null   // ACT_OUT × ACT_H2
    private var aB1: FloatArray? = null   // ACT_H1
    private var aB2: FloatArray? = null   // ACT_H2
    private var aBo: FloatArray? = null   // ACT_OUT

    // ── Critic weights ────────────────────────────────────────────────────────
    private var cW1: FloatArray? = null   // CRIT_H1 × INPUT_DIM
    private var cWo: FloatArray? = null   // CRIT_OUT × CRIT_H1
    private var cB1: FloatArray? = null   // CRIT_H1
    private var cBo: FloatArray? = null   // CRIT_OUT

    // ── Adam state (actor) ────────────────────────────────────────────────────
    private var maW1: FloatArray? = null; private var vaW1: FloatArray? = null
    private var maW2: FloatArray? = null; private var vaW2: FloatArray? = null
    private var maWo: FloatArray? = null; private var vaWo: FloatArray? = null
    private var maB1: FloatArray? = null; private var vaB1: FloatArray? = null
    private var maB2: FloatArray? = null; private var vaB2: FloatArray? = null
    private var maBo: FloatArray? = null; private var vaBo: FloatArray? = null
    private var adamActorStep = 0

    // ── Adam state (critic) ───────────────────────────────────────────────────
    private var mcW1: FloatArray? = null; private var vcW1: FloatArray? = null
    private var mcWo: FloatArray? = null; private var vcWo: FloatArray? = null
    private var mcB1: FloatArray? = null; private var vcB1: FloatArray? = null
    private var mcBo: FloatArray? = null; private var vcBo: FloatArray? = null
    private var adamCriticStep = 0

    // Shared RNG — never allocate inside a hot path
    private val rng = java.util.Random()

    // ── Rollout buffer ────────────────────────────────────────────────────────
    private data class Step(
        val state:   FloatArray,
        val action:  Int,
        val logProb: Float,   // log π_old(a|s) at collection time
        val reward:  Float,
        val value:   Float,   // V(s) at collection time
        val done:    Boolean
    )

    private val rollout    = mutableListOf<Step>()
    private var advantages = FloatArray(0)
    private var returns    = FloatArray(0)

    // ─── Load / save ──────────────────────────────────────────────────────────

    fun load(context: Context) {
        val dir = rlDir(context)
        val actorFile  = File(dir, "ppo_actor.bin")
        val criticFile = File(dir, "ppo_critic.bin")

        if (actorFile.exists()  && actorFile.length()  > 100L) loadActor(actorFile)
        else initActorRandom()

        if (criticFile.exists() && criticFile.length() > 100L) loadCritic(criticFile)
        else initCriticRandom()

        initAdamState()
        isInitialized = true
        Log.i(TAG, "PpoNetwork loaded (totalUpdates=$totalUpdates)")
    }

    fun save(context: Context) {
        if (!isInitialized) return
        try {
            val dir = rlDir(context)
            saveActor(File(dir, "ppo_actor.bin"))
            saveCritic(File(dir, "ppo_critic.bin"))
            Log.i(TAG, "PpoNetwork saved (totalUpdates=$totalUpdates)")
        } catch (e: Exception) {
            Log.e(TAG, "Save failed: ${e.message}")
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /** Clear the current rollout buffer. Call at the start of each rollout. */
    fun beginRollout() { rollout.clear() }

    /**
     * Sample an action from the actor.
     * @return Triple(actionIndex, logProb, stateValue)
     */
    fun selectAction(screenEmb: FloatArray, goalEmb: FloatArray): Triple<Int, Float, Float> {
        if (!isInitialized) return Triple(0, 0f, 0f)
        val state  = concat(screenEmb, goalEmb)
        val probs  = actorForward(state)
        val action = sampleCategorical(probs)
        val lp     = ln(probs[action].toDouble().coerceAtLeast(1e-10)).toFloat()
        val value  = criticForward(state)[0]
        return Triple(action, lp, value)
    }

    /** Store one step in the rollout buffer. */
    fun storeStep(
        screenEmb: FloatArray, goalEmb: FloatArray,
        action: Int, logProb: Float, reward: Float, value: Float, done: Boolean
    ) {
        rollout += Step(concat(screenEmb, goalEmb), action.coerceIn(0, ACT_OUT - 1),
                        logProb, reward, value, done)
    }

    /**
     * Finalize the rollout, compute GAE advantages + returns, then run K PPO epochs.
     * @param lastValue V(s_T) bootstrap value (0 if terminal)
     * @return mean combined loss over K epochs
     */
    fun endRollout(lastValue: Float): Double {
        if (!isInitialized || rollout.isEmpty()) return 0.0
        computeGAE(lastValue)
        var totalLoss = 0.0
        repeat(K_EPOCHS) { totalLoss += trainOneEpoch() }
        rollout.clear()
        totalUpdates++
        return totalLoss / K_EPOCHS
    }

    // ─── GAE ──────────────────────────────────────────────────────────────────

    private fun computeGAE(lastV: Float) {
        val T = rollout.size
        advantages = FloatArray(T)
        returns    = FloatArray(T)
        var lastGae  = 0f
        var nextV    = lastV
        for (t in T - 1 downTo 0) {
            val step    = rollout[t]
            val maskNext = if (step.done) 0f else 1f
            val delta   = step.reward + GAMMA * nextV * maskNext - step.value
            lastGae     = delta + GAMMA * LAMBDA * maskNext * lastGae
            advantages[t] = lastGae
            returns[t]    = lastGae + step.value
            nextV         = step.value
        }
        // Normalize advantages
        val mean = advantages.average().toFloat()
        val std  = sqrt(advantages.map { (it - mean) * (it - mean) }.average().toFloat())
            .coerceAtLeast(1e-8f)
        for (i in advantages.indices) advantages[i] = (advantages[i] - mean) / std
    }

    // ─── PPO training epoch ───────────────────────────────────────────────────

    private fun trainOneEpoch(): Double {
        val T = rollout.size
        var totalActorLoss  = 0.0
        var totalCriticLoss = 0.0
        var totalEntropy    = 0.0

        // Accumulate gradients for the entire rollout in one epoch
        val dAW1 = FloatArray(ACT_H1 * INPUT_DIM)
        val dAW2 = FloatArray(ACT_H2 * ACT_H1)
        val dAWo = FloatArray(ACT_OUT * ACT_H2)
        val dAB1 = FloatArray(ACT_H1)
        val dAB2 = FloatArray(ACT_H2)
        val dABo = FloatArray(ACT_OUT)

        val dCW1 = FloatArray(CRIT_H1 * INPUT_DIM)
        val dCWo = FloatArray(CRIT_OUT * CRIT_H1)
        val dCB1 = FloatArray(CRIT_H1)
        val dCBo = FloatArray(CRIT_OUT)

        for (t in 0 until T) {
            val step = rollout[t]
            val adv  = advantages[t]
            val ret  = returns[t]

            // ── Actor forward ────────────────────────────────────────────────
            val (newProbs, aH1, aH2) = actorForwardWithActs(step.state)
            val newLogProb = ln(newProbs[step.action].toDouble().coerceAtLeast(1e-10)).toFloat()

            // ── PPO ratio r_t = π_new / π_old ────────────────────────────────
            val ratio    = exp((newLogProb - step.logProb).toDouble()).toFloat()
            val clipped  = ratio.coerceIn(1f - CLIP_EPS, 1f + CLIP_EPS)
            val ppoLoss  = -minOf(ratio * adv, clipped * adv)
            totalActorLoss += ppoLoss

            // ── Entropy ───────────────────────────────────────────────────────
            val entropy = -newProbs.sumOf { p ->
                if (p > 1e-10f) p.toDouble() * ln(p.toDouble()) else 0.0
            }
            totalEntropy += entropy

            // ── Combined actor gradient at output logits ──────────────────────
            //
            // Loss to MINIMISE:  L = -min(r*A, clip(r)*A)  -  c_e * H
            //
            // PPO-Clip gradient (chain rule through log-prob → softmax):
            //   dL/d_logit_i = A * r * (π_i - δ_{a,i})   when NOT clipped
            //   dL/d_logit_i = 0                          when clipped
            //   (clipping = the min selects the constant clip*A term, so ∂/∂r = 0)
            //
            // Entropy gradient (chain rule through softmax):
            //   H = -Σ π_j log π_j
            //   d(-c_e*H)/d_logit_i = c_e * π_i * (log π_i + H)
            //
            val isClipped = (ratio * adv) > (clipped * adv)
            val H = entropy.toFloat()
            val actorDeltaOut = FloatArray(ACT_OUT) { i ->
                val ppoPart = if (isClipped) 0f
                              else adv * ratio * (newProbs[i] - if (i == step.action) 1f else 0f)
                val entropyPart = ENTROPY_COEFF * newProbs[i] *
                    (ln(newProbs[i].toDouble().coerceAtLeast(1e-10)).toFloat() + H)
                ppoPart + entropyPart
            }
            accumulateActorGradients(actorDeltaOut, aH1, aH2, step.state,
                                     dAW1, dAW2, dAWo, dAB1, dAB2, dABo)

            // ── Critic forward + value loss ───────────────────────────────────
            val (vPred, cH1) = criticForwardWithActs(step.state)
            val vErr    = vPred[0] - ret
            val critLoss = VALUE_COEFF * vErr * vErr
            totalCriticLoss += critLoss.toDouble()

            val dVo = FloatArray(CRIT_OUT) { 2f * VALUE_COEFF * vErr / T }
            accumulateCriticGradients(dVo, cH1, step.state,
                                      dCW1, dCWo, dCB1, dCBo)
        }

        // Scale gradients by 1/T
        val invT = 1f / T
        for (arr in listOf(dAW1, dAW2, dAWo, dAB1, dAB2, dABo)) arr.forEachIndexed { i, v -> arr[i] = v * invT }
        for (arr in listOf(dCW1, dCWo, dCB1, dCBo))              arr.forEachIndexed { i, v -> arr[i] = v * invT }

        // Clip gradients by global norm
        clipGradients(MAX_GRAD_NORM, dAW1, dAW2, dAWo, dAB1, dAB2, dABo)
        clipGradients(MAX_GRAD_NORM, dCW1, dCWo, dCB1, dCBo)

        // ── Adam updates ──────────────────────────────────────────────────────
        adamActorStep++
        adamUpdate(aW1!!, dAW1, maW1!!, vaW1!!, adamActorStep, LR_ACTOR)
        adamUpdate(aW2!!, dAW2, maW2!!, vaW2!!, adamActorStep, LR_ACTOR)
        adamUpdate(aWo!!, dAWo, maWo!!, vaWo!!, adamActorStep, LR_ACTOR)
        adamUpdate(aB1!!, dAB1, maB1!!, vaB1!!, adamActorStep, LR_ACTOR)
        adamUpdate(aB2!!, dAB2, maB2!!, vaB2!!, adamActorStep, LR_ACTOR)
        adamUpdate(aBo!!, dABo, maBo!!, vaBo!!, adamActorStep, LR_ACTOR)

        adamCriticStep++
        adamUpdate(cW1!!, dCW1, mcW1!!, vcW1!!, adamCriticStep, LR_CRITIC)
        adamUpdate(cWo!!, dCWo, mcWo!!, vcWo!!, adamCriticStep, LR_CRITIC)
        adamUpdate(cB1!!, dCB1, mcB1!!, vcB1!!, adamCriticStep, LR_CRITIC)
        adamUpdate(cBo!!, dCBo, mcBo!!, vcBo!!, adamCriticStep, LR_CRITIC)

        lastActorLoss  = totalActorLoss  / T
        lastCriticLoss = totalCriticLoss / T
        lastEntropy    = totalEntropy    / T

        return lastActorLoss + lastCriticLoss - ENTROPY_COEFF * lastEntropy
    }

    // ─── Actor forward ─────────────────────────────────────────────────────────

    private fun actorForward(state: FloatArray): FloatArray {
        val h1 = matVecRelu(aW1!!, aB1!!, state, ACT_H1, INPUT_DIM)
        val h2 = matVecRelu(aW2!!, aB2!!, h1,    ACT_H2, ACT_H1)
        val logits = matVecLinear(aWo!!, aBo!!, h2, ACT_OUT, ACT_H2)
        return softmax(logits)
    }

    private fun actorForwardWithActs(state: FloatArray): Triple<FloatArray, FloatArray, FloatArray> {
        val h1 = matVecRelu(aW1!!, aB1!!, state, ACT_H1, INPUT_DIM)
        val h2 = matVecRelu(aW2!!, aB2!!, h1,    ACT_H2, ACT_H1)
        val logits = matVecLinear(aWo!!, aBo!!, h2, ACT_OUT, ACT_H2)
        return Triple(softmax(logits), h1, h2)
    }

    // ─── Critic forward ────────────────────────────────────────────────────────

    private fun criticForward(state: FloatArray): FloatArray {
        val h1 = matVecRelu(cW1!!, cB1!!, state, CRIT_H1, INPUT_DIM)
        return matVecLinear(cWo!!, cBo!!, h1, CRIT_OUT, CRIT_H1)
    }

    private fun criticForwardWithActs(state: FloatArray): Pair<FloatArray, FloatArray> {
        val h1 = matVecRelu(cW1!!, cB1!!, state, CRIT_H1, INPUT_DIM)
        return Pair(matVecLinear(cWo!!, cBo!!, h1, CRIT_OUT, CRIT_H1), h1)
    }

    // ─── Gradient accumulation helpers ────────────────────────────────────────

    private fun accumulateActorGradients(
        dOut: FloatArray, h1: FloatArray, h2: FloatArray, state: FloatArray,
        dW1: FloatArray, dW2: FloatArray, dWo: FloatArray,
        dB1: FloatArray, dB2: FloatArray, dBo: FloatArray
    ) {
        for (i in 0 until ACT_OUT) {
            dBo[i] += dOut[i]
            for (j in 0 until ACT_H2) dWo[i * ACT_H2 + j] += dOut[i] * h2[j]
        }
        val dH2 = FloatArray(ACT_H2) { j ->
            var d = 0f
            for (i in 0 until ACT_OUT) d += aWo!![i * ACT_H2 + j] * dOut[i]
            if (h2[j] > 0f) d else 0f
        }
        for (i in 0 until ACT_H2) {
            dB2[i] += dH2[i]
            for (j in 0 until ACT_H1) dW2[i * ACT_H1 + j] += dH2[i] * h1[j]
        }
        val dH1 = FloatArray(ACT_H1) { j ->
            var d = 0f
            for (i in 0 until ACT_H2) d += aW2!![i * ACT_H1 + j] * dH2[i]
            if (h1[j] > 0f) d else 0f
        }
        for (i in 0 until ACT_H1) {
            dB1[i] += dH1[i]
            for (j in 0 until INPUT_DIM) dW1[i * INPUT_DIM + j] += dH1[i] * state[j]
        }
    }

    private fun accumulateCriticGradients(
        dOut: FloatArray, h1: FloatArray, state: FloatArray,
        dW1: FloatArray, dWo: FloatArray, dB1: FloatArray, dBo: FloatArray
    ) {
        for (i in 0 until CRIT_OUT) {
            dBo[i] += dOut[i]
            for (j in 0 until CRIT_H1) dWo[i * CRIT_H1 + j] += dOut[i] * h1[j]
        }
        val dH1 = FloatArray(CRIT_H1) { j ->
            var d = 0f
            for (i in 0 until CRIT_OUT) d += cWo!![i * CRIT_H1 + j] * dOut[i]
            if (h1[j] > 0f) d else 0f
        }
        for (i in 0 until CRIT_H1) {
            dB1[i] += dH1[i]
            for (j in 0 until INPUT_DIM) dW1[i * INPUT_DIM + j] += dH1[i] * state[j]
        }
    }

    // ─── Math helpers ─────────────────────────────────────────────────────────

    private fun matVecRelu(W: FloatArray, b: FloatArray, x: FloatArray, rows: Int, cols: Int): FloatArray =
        FloatArray(rows) { i ->
            var s = b[i]
            for (j in 0 until cols) s += W[i * cols + j] * x[j]
            if (s > 0f) s else 0f
        }

    private fun matVecLinear(W: FloatArray, b: FloatArray, x: FloatArray, rows: Int, cols: Int): FloatArray =
        FloatArray(rows) { i ->
            var s = b[i]
            for (j in 0 until cols) s += W[i * cols + j] * x[j]
            s
        }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxL = logits.max()
        val exp  = FloatArray(logits.size) { exp((logits[it] - maxL).toDouble()).toFloat() }
        val sum  = exp.sum()
        return FloatArray(exp.size) { exp[it] / sum }
    }

    private fun sampleCategorical(probs: FloatArray): Int {
        var r = rng.nextFloat()
        for (i in probs.indices) { r -= probs[i]; if (r <= 0f) return i }
        return probs.size - 1
    }

    private fun concat(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(INPUT_DIM)
        System.arraycopy(a, 0, out, 0,   minOf(a.size, 128))
        System.arraycopy(b, 0, out, 128, minOf(b.size, 128))
        return out
    }

    private fun clipGradients(maxNorm: Float, vararg arrays: FloatArray) {
        var norm = 0.0
        arrays.forEach { a -> a.forEach { norm += it.toDouble() * it } }
        norm = sqrt(norm)
        if (norm > maxNorm) {
            val scale = maxNorm / norm.toFloat()
            arrays.forEach { a -> a.forEachIndexed { i, v -> a[i] = v * scale } }
        }
    }

    private fun adamUpdate(W: FloatArray, g: FloatArray, m: FloatArray, v: FloatArray, t: Int, lr: Float) {
        val bc1 = 1f - BETA1.pow(t)
        val bc2 = 1f - BETA2.pow(t)
        for (i in W.indices) {
            m[i] = BETA1 * m[i] + (1f - BETA1) * g[i]
            v[i] = BETA2 * v[i] + (1f - BETA2) * g[i] * g[i]
            W[i] -= lr * (m[i] / bc1) / (sqrt((v[i] / bc2).toDouble()).toFloat() + ADAM_EPS)
        }
    }

    private fun Float.pow(n: Int): Float { var r = 1f; repeat(n.coerceAtMost(200)) { r *= this }; return r }

    // ─── Initialization ────────────────────────────────────────────────────────

    private fun initActorRandom() {
        val rng = java.util.Random(77L)
        fun xavier(r: Int, c: Int) = FloatArray(r * c) { (rng.nextGaussian() * sqrt(2.0 / (r + c))).toFloat() }
        aW1 = xavier(ACT_H1, INPUT_DIM); aB1 = FloatArray(ACT_H1)
        aW2 = xavier(ACT_H2, ACT_H1);   aB2 = FloatArray(ACT_H2)
        aWo = xavier(ACT_OUT, ACT_H2);  aBo = FloatArray(ACT_OUT)
    }

    private fun initCriticRandom() {
        val rng = java.util.Random(88L)
        fun xavier(r: Int, c: Int) = FloatArray(r * c) { (rng.nextGaussian() * sqrt(2.0 / (r + c))).toFloat() }
        cW1 = xavier(CRIT_H1, INPUT_DIM); cB1 = FloatArray(CRIT_H1)
        cWo = xavier(CRIT_OUT, CRIT_H1);  cBo = FloatArray(CRIT_OUT)
    }

    private fun initAdamState() {
        maW1 = FloatArray(ACT_H1 * INPUT_DIM); vaW1 = FloatArray(ACT_H1 * INPUT_DIM)
        maW2 = FloatArray(ACT_H2 * ACT_H1);   vaW2 = FloatArray(ACT_H2 * ACT_H1)
        maWo = FloatArray(ACT_OUT * ACT_H2);   vaWo = FloatArray(ACT_OUT * ACT_H2)
        maB1 = FloatArray(ACT_H1);             vaB1 = FloatArray(ACT_H1)
        maB2 = FloatArray(ACT_H2);             vaB2 = FloatArray(ACT_H2)
        maBo = FloatArray(ACT_OUT);            vaBo = FloatArray(ACT_OUT)
        adamActorStep = 0

        mcW1 = FloatArray(CRIT_H1 * INPUT_DIM); vcW1 = FloatArray(CRIT_H1 * INPUT_DIM)
        mcWo = FloatArray(CRIT_OUT * CRIT_H1);  vcWo = FloatArray(CRIT_OUT * CRIT_H1)
        mcB1 = FloatArray(CRIT_H1);             vcB1 = FloatArray(CRIT_H1)
        mcBo = FloatArray(CRIT_OUT);            vcBo = FloatArray(CRIT_OUT)
        adamCriticStep = 0
    }

    // ─── Persistence ──────────────────────────────────────────────────────────

    private fun saveActor(file: File) {
        DataOutputStream(FileOutputStream(file)).use { out ->
            out.writeInt(adamActorStep); out.writeInt(totalUpdates)
            for (arr in listOf(aW1!!, aW2!!, aWo!!, aB1!!, aB2!!, aBo!!)) {
                out.writeInt(arr.size); arr.forEach { out.writeFloat(it) }
            }
        }
    }

    private fun saveCritic(file: File) {
        DataOutputStream(FileOutputStream(file)).use { out ->
            out.writeInt(adamCriticStep)
            for (arr in listOf(cW1!!, cWo!!, cB1!!, cBo!!)) {
                out.writeInt(arr.size); arr.forEach { out.writeFloat(it) }
            }
        }
    }

    private fun loadActor(file: File) {
        try {
            DataInputStream(FileInputStream(file)).use { din ->
                adamActorStep = din.readInt(); totalUpdates = din.readInt()
                val arrays = (1..6).map { FloatArray(din.readInt()) { din.readFloat() } }
                aW1 = arrays[0]; aW2 = arrays[1]; aWo = arrays[2]
                aB1 = arrays[3]; aB2 = arrays[4]; aBo = arrays[5]
            }
        } catch (e: Exception) {
            Log.w(TAG, "Actor load failed: ${e.message}"); initActorRandom()
        }
    }

    private fun loadCritic(file: File) {
        try {
            DataInputStream(FileInputStream(file)).use { din ->
                adamCriticStep = din.readInt()
                val arrays = (1..4).map { FloatArray(din.readInt()) { din.readFloat() } }
                cW1 = arrays[0]; cWo = arrays[1]; cB1 = arrays[2]; cBo = arrays[3]
            }
        } catch (e: Exception) {
            Log.w(TAG, "Critic load failed: ${e.message}"); initCriticRandom()
        }
    }

    private fun rlDir(context: Context): File =
        File(context.filesDir, "rl").also { it.mkdirs() }
            .let { if (it.canWrite()) it else (context.getExternalFilesDir("rl") ?: it).also { d -> d.mkdirs() } }
}
