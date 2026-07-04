package com.jekael.adoel.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.jekael.adoel.data.DoffRepository

val mcNoKey = ActionParameters.Key<String>("mcNo")

/** Records a plain doff for the tapped machine directly from the widget, no app needed. Skips the
 * in-app confirm dialog (Glance can't render a Compose dialog) — same no-confirmation precedent
 * as the notification's "Doff" action button. */
class DoffActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mcNo = parameters[mcNoKey] ?: return
        DoffRepository(context).quickDoff(mcNo)
    }
}

/** Deletes a pending estimasi directly from the widget, no app/confirm dialog needed — mistaken
 * taps just mean retyping the console command, which is cheap. */
class HapusActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mcNo = parameters[mcNoKey] ?: return
        DoffRepository(context).hapusEstimasi(mcNo)
    }
}
