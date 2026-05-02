package com.ariaagent.mobile

import com.ariaagent.mobile.ui.viewmodel.SessionStatsUiState
import org.junit.Assert.*
import org.junit.Test

class SessionStatsUiStateTest {

    @Test
    fun `successRate is zero when no tasks have run`() {
        val stats = SessionStatsUiState()
        assertEquals(0f, stats.successRate, 0.001f)
    }

    @Test
    fun `successRate is 1 when all tasks succeeded`() {
        val stats = SessionStatsUiState(tasksCompleted = 5, tasksErrored = 0)
        assertEquals(1f, stats.successRate, 0.001f)
    }

    @Test
    fun `successRate is 0 when all tasks errored`() {
        val stats = SessionStatsUiState(tasksCompleted = 0, tasksErrored = 3)
        assertEquals(0f, stats.successRate, 0.001f)
    }

    @Test
    fun `successRate is 0_5 for equal completed and errored`() {
        val stats = SessionStatsUiState(tasksCompleted = 4, tasksErrored = 4)
        assertEquals(0.5f, stats.successRate, 0.001f)
    }

    @Test
    fun `avgStepsPerTask is zero when no tasks completed`() {
        val stats = SessionStatsUiState(tasksCompleted = 0, totalSteps = 100)
        assertEquals(0f, stats.avgStepsPerTask, 0.001f)
    }

    @Test
    fun `avgStepsPerTask divides totalSteps by tasksCompleted`() {
        val stats = SessionStatsUiState(tasksCompleted = 4, totalSteps = 20)
        assertEquals(5f, stats.avgStepsPerTask, 0.001f)
    }

    @Test
    fun `sessionDurationMinutes reflects elapsed time`() {
        val startMs = System.currentTimeMillis() - 130_000L // 2+ minutes ago
        val stats = SessionStatsUiState(sessionStartMs = startMs)
        assertTrue("expected ≥ 2 min", stats.sessionDurationMinutes >= 2L)
    }

    @Test
    fun `copy preserves all fields`() {
        val original = SessionStatsUiState(tasksCompleted = 3, tasksErrored = 1, totalSteps = 18)
        val updated  = original.copy(tasksCompleted = 4, totalSteps = 24)
        assertEquals(4, updated.tasksCompleted)
        assertEquals(1, updated.tasksErrored)
        assertEquals(24, updated.totalSteps)
    }
}
