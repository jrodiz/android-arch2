package com.rodiz.arch2.core.common.logging

/**
 * Thin, JVM-only abstraction over a crash-reporting backend (Firebase Crashlytics
 * in production, a no-op in tests / debug builds).
 *
 * Lives in `:core:common` so JVM `:domain` modules can report handled errors
 * without dragging Firebase onto their classpath.
 */
interface CrashReporter {
    /**
     * Tag the current user on subsequent reports. Pass `null` on sign-out to
     * detach the previous identity so the next crash isn't attributed to a
     * user who is no longer signed in.
     */
    fun setUserId(uid: String?)

    /**
     * Breadcrumb-style log line attached to the next crash report. Cheap;
     * sprinkle freely at notable transitions.
     */
    fun log(message: String)

    /**
     * Report a handled exception as a non-fatal. Optional [message] is logged
     * as a breadcrumb immediately before the exception so the dashboard shows
     * what the app was trying to do when it failed.
     */
    fun recordException(throwable: Throwable, message: String? = null)
}

/** Drop-in for tests and the debug build's local Hilt graph. */
object NoOpCrashReporter : CrashReporter {
    override fun setUserId(uid: String?) = Unit
    override fun log(message: String) = Unit
    override fun recordException(throwable: Throwable, message: String?) = Unit
}
