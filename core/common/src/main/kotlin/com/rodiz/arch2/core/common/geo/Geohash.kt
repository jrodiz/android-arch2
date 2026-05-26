package com.rodiz.arch2.core.common.geo

/**
 * Pure-Kotlin geohash encoder. Encodes (lat, lng) to a base-32 geohash string of the
 * requested precision. Standard algorithm — interleaves longitude (even bits) and
 * latitude (odd bits), groups every 5 bits into a base-32 character.
 *
 * Precision rule-of-thumb (full geohash length → approximate cell size):
 *   precision 4 → ~20 km × 20 km
 *   precision 5 → ~4.9 km × 4.9 km
 *   precision 6 → ~1.2 km × 0.6 km   ← TinPet default
 *   precision 7 → ~150 m × 150 m
 */
object Geohash {

    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"

    fun encode(latitude: Double, longitude: Double, precision: Int = 6): String {
        require(precision in 1..12) { "precision must be 1..12, was $precision" }
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }

        var latLo = -90.0
        var latHi = 90.0
        var lngLo = -180.0
        var lngHi = 180.0
        val out = StringBuilder(precision)
        var bit = 0
        var ch = 0
        var even = true // even step → longitude, odd step → latitude

        while (out.length < precision) {
            if (even) {
                val mid = (lngLo + lngHi) / 2
                if (longitude >= mid) {
                    ch = (ch shl 1) or 1
                    lngLo = mid
                } else {
                    ch = ch shl 1
                    lngHi = mid
                }
            } else {
                val mid = (latLo + latHi) / 2
                if (latitude >= mid) {
                    ch = (ch shl 1) or 1
                    latLo = mid
                } else {
                    ch = ch shl 1
                    latHi = mid
                }
            }
            even = !even
            bit++
            if (bit == 5) {
                out.append(BASE32[ch])
                bit = 0
                ch = 0
            }
        }
        return out.toString()
    }
}
