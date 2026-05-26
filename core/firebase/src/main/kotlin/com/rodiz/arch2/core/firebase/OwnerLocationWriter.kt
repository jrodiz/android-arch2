package com.rodiz.arch2.core.firebase

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.common.geo.Geohash
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint

/**
 * Cross-feature writer for the signed-in owner's location on `/owners/{uid}`.
 *
 * Exists because :feature:notifications:presentation needs to persist the
 * onboarding location-permission grant, but cannot depend on
 * :feature:profile:domain.UpdateLocationUseCase (architecture rule:
 * presentation modules may only depend on another feature's :nav). Living in
 * :core/firebase makes it consumable by any module that already has the
 * Firebase classpath without crossing the feature boundary.
 *
 * Writes are intentionally a strict mirror of
 * `FirestoreOwnerProfileRepository.updateLocation(...)` — same path, same
 * field set, same SetOptions.merge() semantics — so the Settings/Profile
 * path and the onboarding path can't drift.
 */
@Singleton
class OwnerLocationWriter @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun updateLocation(lat: Double, lng: Double, cityLabel: String?) = withContext(io) {
        val uid = firebaseAuth.currentUser?.uid ?: error("No signed-in user")
        val now = Clock.System.now()
        firestore.collection("owners").document(uid).set(
            mapOf(
                "location" to FirestoreGeoPoint(lat, lng),
                "geohash" to Geohash.encode(lat, lng, precision = 6),
                "cityLabel" to cityLabel,
                "updatedAt" to now.toTimestamp(),
                "createdAt" to now.toTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    private fun kotlinx.datetime.Instant.toTimestamp(): Timestamp =
        Timestamp(Date.from(toJavaInstant()))
}
