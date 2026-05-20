package com.rodiz.arch2.feature.deck.domain.model

enum class DistanceBucket(val label: String) {
    UNDER_5_KM("< 5 km"),
    BUCKET_5_15_KM("5–15 km"),
    BUCKET_15_50_KM("15–50 km"),
    OVER_50_KM("50+ km");

    companion object {
        fun fromKm(km: Double): DistanceBucket = when {
            km < 5 -> UNDER_5_KM
            km < 15 -> BUCKET_5_15_KM
            km < 50 -> BUCKET_15_50_KM
            else -> OVER_50_KM
        }
    }
}
