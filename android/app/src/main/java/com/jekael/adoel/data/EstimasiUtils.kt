package com.jekael.adoel.data

import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Compose-free "which machine is next" logic, shared by MainScreen's RadarCard list and the
 * home-screen widget (which runs in its own Glance composition and can't reuse MainScreen's
 * inline Compose state/remember blocks). Juga rumah bagi rumus estimasi murni per tipe mesin,
 * dipisah dari tokenizing/mutasi state di DoffViewModel supaya bisa diuji numerik langsung.
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

/** D405: sisa menit sampai doff dari bacaan yard berjalan — mesin menggulung [speedYardPerMin]
 * yard tiap menit, jadi sisa (target − berjalan) dibagi kecepatan. Pemanggil wajib menjamin
 * speed > 0 (DoffViewModel menolak input sebelum sampai sini). */
fun sisaMenitD405(targetYard: Double, yardBerjalan: Double, speedYardPerMin: Double): Int =
    ((targetYard - yardBerjalan) / speedYardPerMin).roundToInt()

/** D408: estimasi absolut dari bacaan jam pada counter mesin (bukan jam dinding!) plus menit
 * koreksi tetap per mesin. [nowEpochMin]/[zone] hanya seam untuk unit test. */
fun estAbsD408(
    jamCounterMin: Int,
    koreksiMin: Double,
    nowEpochMin: Long = nowAbsMin(),
    zone: TimeZone = TimeZone.getDefault(),
): Long = jamKeShiftAbs(jamCounterMin, nowEpochMin, zone) + koreksiMin.roundToInt()

/** Token yard pada perintah DOFFING: "+5" berarti delta dari target standar (5 yard melewati
 * target), "295"/"295y" berarti nilai absolut. Delta tanpa target standar jatuh ke absolut. */
fun resolveYardToken(isDelta: Boolean, value: Double, standardYard: Double?): Double =
    if (isDelta && standardYard != null) standardYard + value else value
