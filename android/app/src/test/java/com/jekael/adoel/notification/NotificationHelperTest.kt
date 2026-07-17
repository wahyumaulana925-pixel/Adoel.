package com.jekael.adoel.notification

import com.jekael.adoel.data.REMINDER_LEAD_MIN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the pure scheduling-decision logic NotificationHelper.scheduleNotif relies on —
 * whether the reminder/ready alarms are still worth scheduling, and that the two alarms for the
 * same mc, plus alarms for different mc's, never collide on notification/PendingIntent id. This
 * is the actual doff-alert feature that operators depend on day to day, so a regression here
 * (e.g. an inverted comparison, or an id collision that makes one alarm silently overwrite
 * another) needs to fail a test, not surface as "an alert never fired" on the factory floor. */
class NotificationHelperTest {

    // ---- shouldScheduleReminder: reminderAt = estAbsMin - REMINDER_LEAD_MIN, fires if > now ----

    @Test
    fun reminderScheduledWhenLeadTimeStillAhead() {
        val now = 1_000L
        val estAbsMin = now + REMINDER_LEAD_MIN + 1
        assertTrue(NotificationHelper.shouldScheduleReminder(estAbsMin, now))
    }

    @Test
    fun reminderNotScheduledOnceLeadTimeHasPassed() {
        val now = 1_000L
        val estAbsMin = now + REMINDER_LEAD_MIN
        assertFalse(NotificationHelper.shouldScheduleReminder(estAbsMin, now))
    }

    @Test
    fun reminderNotScheduledForAlreadyOverdueEstimate() {
        val now = 1_000L
        assertFalse(NotificationHelper.shouldScheduleReminder(now - 50, now))
    }

    // ---- shouldScheduleReady: fires if estAbsMin > now ----

    @Test
    fun readyScheduledWhenEstimateStillInFuture() {
        val now = 1_000L
        assertTrue(NotificationHelper.shouldScheduleReady(now + 1, now))
    }

    @Test
    fun readyNotScheduledAtOrAfterEstimate() {
        val now = 1_000L
        assertFalse(NotificationHelper.shouldScheduleReady(now, now))
        assertFalse(NotificationHelper.shouldScheduleReady(now - 1, now))
    }

    // ---- notifIdFor: reminder/ready for the same mc, and different mc's, must not collide ----

    @Test
    fun reminderAndReadyIdsForSameMachineDiffer() {
        val readyId = NotificationHelper.notifIdFor("29", isReminder = false)
        val reminderId = NotificationHelper.notifIdFor("29", isReminder = true)
        assertNotEquals(readyId, reminderId)
    }

    @Test
    fun idsForDifferentMachinesDoNotCollide() {
        val mc29 = NotificationHelper.notifIdFor("29", isReminder = false)
        val mc61 = NotificationHelper.notifIdFor("61", isReminder = false)
        assertNotEquals(mc29, mc61)

        val mc29Reminder = NotificationHelper.notifIdFor("29", isReminder = true)
        val mc61Reminder = NotificationHelper.notifIdFor("61", isReminder = true)
        assertNotEquals(mc29Reminder, mc61Reminder)
    }

    @Test
    fun idIsStableForSameInputs() {
        assertEquals(
            NotificationHelper.notifIdFor("29", isReminder = true),
            NotificationHelper.notifIdFor("29", isReminder = true),
        )
    }
}
