package dev.minimalist.launcher.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [Result] sealed class.
 *
 * Tests cover the core contract:
 * - success/error factory functions
 * - [Result.isSuccess] / [Result.isError] properties
 * - [map] transformation
 * - [getOrNull] / [getOrDefault]
 * - [runCatchingResult] wrapping
 */
class ResultTest {

    // ── Factory functions ─────────────────────────────────────────────────────

    @Test
    fun `success factory creates Success with correct data`() {
        val result = Result.success(42)
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo(42)
    }

    @Test
    fun `error factory with exception creates Error`() {
        val ex = RuntimeException("fail")
        val result = Result.error(ex)
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).exception).isEqualTo(ex)
    }

    @Test
    fun `error factory with message creates Error`() {
        val result = Result.error("something went wrong")
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).isEqualTo("something went wrong")
    }

    // ── isSuccess / isError ───────────────────────────────────────────────────

    @Test
    fun `isSuccess is true for Success`() {
        assertThat(Result.success("x").isSuccess).isTrue()
    }

    @Test
    fun `isSuccess is false for Error`() {
        assertThat(Result.error("e").isSuccess).isFalse()
    }

    @Test
    fun `isError is true for Error`() {
        assertThat(Result.error("e").isError).isTrue()
    }

    @Test
    fun `isError is false for Success`() {
        assertThat(Result.success(1).isError).isFalse()
    }

    // ── map ───────────────────────────────────────────────────────────────────

    @Test
    fun `map transforms Success value`() {
        val result = Result.success(10).map { it * 2 }
        assertThat((result as Result.Success).data).isEqualTo(20)
    }

    @Test
    fun `map passes Error through unchanged`() {
        val ex = IllegalStateException("oops")
        val result: Result<Int> = Result.error(ex)
        val mapped = result.map { it * 2 }
        assertThat(mapped).isInstanceOf(Result.Error::class.java)
        assertThat((mapped as Result.Error).exception).isEqualTo(ex)
    }

    // ── getOrNull / getOrDefault ───────────────────────────────────────────────

    @Test
    fun `getOrNull returns data for Success`() {
        assertThat(Result.success("hello").getOrNull()).isEqualTo("hello")
    }

    @Test
    fun `getOrNull returns null for Error`() {
        val result: String? = Result.error("e").getOrNull()
        assertThat(result).isNull()
    }

    @Test
    fun `getOrDefault returns data for Success`() {
        assertThat(Result.success(5).getOrDefault(99)).isEqualTo(5)
    }

    @Test
    fun `getOrDefault returns default for Error`() {
        assertThat(Result.error("e").getOrDefault(99)).isEqualTo(99)
    }

    // ── runCatchingResult ─────────────────────────────────────────────────────

    @Test
    fun `runCatchingResult wraps successful block in Success`() {
        val result = runCatchingResult { 42 }
        assertThat((result as Result.Success).data).isEqualTo(42)
    }

    @Test
    fun `runCatchingResult wraps thrown Exception in Error`() {
        val result = runCatchingResult<Int> { throw IllegalArgumentException("bad") }
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).exception).isInstanceOf(IllegalArgumentException::class.java)
    }
}
