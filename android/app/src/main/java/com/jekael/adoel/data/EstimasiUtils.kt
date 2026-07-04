package com.jekael.adoel.data

/**
 * Compose-free "which machine is next" logic, shared by MainScreen's RadarCard list and the
 * home-screen widget (which runs in its own Glance composition and can't reuse MainScreen's
 * inline Compose state/remember blocks).
 */
enum class UrgencyLevel { CALM, SOON, IMMINENT, OVERDUE }

fun urgencyLevel(remainingMin: Long): UrgencyLevel = when {
    remainingMin > 30 -> UrgencyLevel.CALM
    remainingMin > 10 -> UrgencyLevel.SOON
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
