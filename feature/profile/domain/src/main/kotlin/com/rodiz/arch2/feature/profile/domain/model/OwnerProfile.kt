package com.rodiz.arch2.feature.profile.domain.model

import kotlinx.datetime.Instant

data class OwnerProfile(
    val id: String,
    val firstName: String,
    val avatarUrl: String?,
    val email: String?,
    val paused: Boolean,
    val location: GeoPoint?,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Free-form self-introduction shown on Edit Profile. Hard-capped at 150 chars. */
    val bio: String = "",
)

data class GeoPoint(
    val lat: Double,
    val lng: Double,
    val geohash: String,
    /** Human-readable label like "Mexico City, MX"; null when reverse-geocode failed. */
    val cityLabel: String? = null,
)
