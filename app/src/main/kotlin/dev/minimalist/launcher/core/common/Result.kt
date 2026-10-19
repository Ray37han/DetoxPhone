package dev.minimalist.launcher.core.common

/**
 * Unified result wrapper for repository and use-case return values.
 *
 * Why this exists:
 * - Repositories interact with PackageManager, Room, and DataStore — all can fail.
 * - A sealed class forces call sites to handle both outcomes explicitly.
 * - Using this instead of exceptions avoids unhandled-exception crashes when,
 *   for example, a package is uninstalled mid-operation or a permission is revoked.
 *
 * Usage:
 * ```kotlin
 * when (val result = repository.getApps()) {
 *     is Result.Success -> render(result.data)
 *     is Result.Error   -> showError(result.exception)
 * }
 * ```
 *
 * Use [Result.success] and [Result.error] factory functions for clarity.
 * Use [map], [getOrNull], and [getOrDefault] for safe transformations.
 */
sealed class Result<out T> {

    data class Success<out T>(val data: T) : Result<T>()

    data class Error(
        val exception: Throwable,
        val message: String = exception.localizedMessage ?: "Unknown error"
    ) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(exception: Throwable): Result<Nothing> = Error(exception)
        fun error(message: String): Result<Nothing> = Error(Exception(message))
    }
}

/** Transform the [Result.Success] value, leaving [Result.Error] unchanged. */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.success(transform(data))
    is Result.Error   -> this
}

/** Returns the success value, or null on error. */
fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.data

/** Returns the success value, or [default] on error. */
fun <T> Result<T>.getOrDefault(default: T): T = getOrNull() ?: default

/**
 * Execute [block] and wrap the outcome in [Result].
 * Catches all [Exception]s; does NOT catch [Error] subclasses (OOM, etc.).
 */
inline fun <T> runCatchingResult(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: Exception) {
    Result.error(e)
}
