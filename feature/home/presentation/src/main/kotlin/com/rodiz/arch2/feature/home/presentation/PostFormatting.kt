package com.rodiz.arch2.feature.home.presentation

import com.rodiz.arch2.feature.home.domain.model.Post
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal fun Post.toUiModel(
    now: Instant = Clock.System.now(),
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): PostUiModel = PostUiModel(
    id = id,
    authorName = author.displayName,
    authorAvatarUrl = author.avatarUrl,
    authorInitial = author.displayName.firstOrNull()?.uppercase() ?: "?",
    relativeTime = formatRelativeTime(createdAt, now, timeZone),
    text = text,
    imageUrl = imageUrl,
    likeCount = reactions.likeCount,
    formattedLikeCount = formatCompactCount(reactions.likeCount),
    commentCount = commentCount,
    formattedCommentCount = formatCompactCount(commentCount),
    shareCount = shareCount,
    formattedShareCount = formatCompactCount(shareCount),
    viewerHasLiked = viewerHasLiked,
)

internal fun formatRelativeTime(
    createdAt: Instant,
    now: Instant,
    timeZone: TimeZone,
): String {
    val delta = now - createdAt
    return when {
        delta < 60.seconds -> "now"
        delta < 60.minutes -> "${delta.inWholeMinutes}m"
        delta < 24.hours -> "${delta.inWholeHours}h"
        delta < 2.days -> "Yesterday"
        delta < 7.days -> "${delta.inWholeDays}d"
        else -> formatAbsoluteDate(createdAt.toLocalDateTime(timeZone))
    }
}

private fun formatAbsoluteDate(dt: LocalDateTime): String =
    "${MONTH_ABBREV[dt.monthNumber - 1]} ${dt.dayOfMonth}"

private val MONTH_ABBREV = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

internal fun formatCompactCount(count: Int): String = when {
    count < 1_000 -> count.toString()
    count < 10_000 -> compactWithDecimal(count, 1_000, "K")
    count < 1_000_000 -> "${count / 1_000}K"
    count < 10_000_000 -> compactWithDecimal(count, 1_000_000, "M")
    else -> "${count / 1_000_000}M"
}

private fun compactWithDecimal(count: Int, divisor: Int, suffix: String): String {
    val whole = count / divisor
    val tenths = (count % divisor) / (divisor / 10)
    return if (tenths == 0) "$whole$suffix" else "$whole.$tenths$suffix"
}
