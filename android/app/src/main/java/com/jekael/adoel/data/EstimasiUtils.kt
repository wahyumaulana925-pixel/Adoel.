package com.jekael.adoel.data

/**
 * Compose-free "which machine is next" logic, shared by MainScreen's RadarCard list and the
 * home-screen widget (which runs in its own Glance composition and can't reuse MainScreen's
 * inline Compose state/remember blocks).
 */
enum class UrgencyLevel { CALM, SOON, IMMINENT, OVERDUE }

/** More than this many minutes remaining reads as "tenang" — no visual urgency at all. */
const val URGENCY_CALM_OVER_MIN = 30L

/** Between this and [URGENCY_CALM_OVER_MIN] minutes remaining is "segera" (amber). Below it,
 * down to zero, is "mendesak" (orange); at/past zero the card is overdue (red, pulsing). */
const val URGENCY_SOON_OVER_MIN = 10L

fun urgencyLevel(remainingMin: Long): UrgencyLevel = when {
    remainingMin > URGENCY_CALM_OVER_MIN -> UrgencyLevel.CALM
    remainingMin > URGENCY_SOON_OVER_MIN -> UrgencyLevel.SOON
    remainingMin > 0 -> UrgencyLevel.IMMINENT
    else -> UrgencyLevel.OVERDUE
}

fun sortedByNearest(estimasi: Map<String, Estimasi>): List<Estimasi> =
    estimasi.values.sortedBy { it.estAbsMin }

fun partitionSegeraMenunggu(sorted: List<Estimasi>, nowAbs: Long): Pair<List<Estimasi>, List<Estimasi>> =
    sorted.partition { it.estAbsMin - nowAbs <= 0 }

/** The single machine most in need of attention right now: earliest overdue, else soonest upcoming. */
fun nearestUpcoming(estimasi: Map<String, Estimasi>, nowAbs: Long): Estimasi? {
    val (segera, menunggu) = partitionSegeraMenunggu(sortedByNearest(estimasi), nowAbs)
    return segera.firstOrNull() ?: menunggu.firstOrNull()
}
