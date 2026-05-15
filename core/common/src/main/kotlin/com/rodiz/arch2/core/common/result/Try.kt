package com.rodiz.arch2.core.common.result

sealed interface Try<out T, out E : AppError> {
    data class Success<T>(val value: T) : Try<T, Nothing>
    data class Failure<E : AppError>(val error: E) : Try<Nothing, E>
}

inline fun <T, E : AppError, R> Try<T, E>.fold(
    onSuccess: (T) -> R,
    onFailure: (E) -> R,
): R = when (this) {
    is Try.Success -> onSuccess(value)
    is Try.Failure -> onFailure(error)
}

inline fun <T, E : AppError, R> Try<T, E>.map(transform: (T) -> R): Try<R, E> = when (this) {
    is Try.Success -> Try.Success(transform(value))
    is Try.Failure -> this
}
