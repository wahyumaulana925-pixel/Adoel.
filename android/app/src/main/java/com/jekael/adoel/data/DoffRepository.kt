package com.jekael.adoel.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("adoel_v5")

private val STATE_KEY = stringPreferencesKey("state_v2")

private data class SerialState(
    val db: Map<String, SerialMesin>,
    val estimasi: Map<String, SerialEstimasi>,
    val aktual: List<SerialAktual>,
    val nextId: Int,
    val quickModeEnabled: Boolean,
)

private data class SerialMesin(
    val tipe: String,
    val corak: String,
    val targetYard: Double?,
    val speed: Double?,
    val koreksi: Double?,
)

private data class SerialEstimasi(
    val mcNo: String,
    val estAbsMin: Long,
    val startAbsMin: Long,
    val corakOverride: String?,
    val yardOverride: Double?,
)

private data class SerialAktual(
    val id: Int,
    val mcNo: String,
    val jam: String,
    val ket: String,
    val corakOverride: String?,
    val customYard: Double?,
)

class DoffRepository(private val context: Context) {
    private val gson: Gson = GsonBuilder().create()

    suspend fun load(): DoffState {
        return try {
            val prefs = context.dataStore.data.first()
            val json = prefs[STATE_KEY] ?: return DoffState(db = buildDefaultDb())
            val serial = gson.fromJson(json, SerialState::class.java) ?: return DoffState(db = buildDefaultDb())
            DoffState(
                db = serial.db.mapValues { (_, v) ->
                    MesinData(
                        tipe = runCatching { MesinTipe.valueOf(v.tipe) }.getOrDefault(MesinTipe.TAPPET),
                        corak = v.corak,
                        targetYard = v.targetYard,
                        speed = v.speed,
                        koreksi = v.koreksi,
                    )
                },
                estimasi = serial.estimasi.mapValues { (_, v) ->
                    Estimasi(v.mcNo, v.estAbsMin, v.startAbsMin, v.corakOverride, v.yardOverride)
                },
                aktual = serial.aktual.map { a ->
                    AktualEntry(a.id, a.mcNo, a.jam, a.ket, a.corakOverride, a.customYard)
                },
                nextId = serial.nextId,
                quickModeEnabled = serial.quickModeEnabled,
            )
        } catch (e: Exception) {
            DoffState(db = buildDefaultDb())
        }
    }

    suspend fun save(state: DoffState) {
        val serial = SerialState(
            db = state.db.mapValues { (_, v) ->
                SerialMesin(v.tipe.name, v.corak, v.targetYard, v.speed, v.koreksi)
            },
            estimasi = state.estimasi.mapValues { (_, v) ->
                SerialEstimasi(v.mcNo, v.estAbsMin, v.startAbsMin, v.corakOverride, v.yardOverride)
            },
            aktual = state.aktual.map { a ->
                SerialAktual(a.id, a.mcNo, a.jam, a.ket, a.corakOverride, a.customYard)
            },
            nextId = state.nextId,
            quickModeEnabled = state.quickModeEnabled,
        )
        context.dataStore.edit { prefs ->
            prefs[STATE_KEY] = gson.toJson(serial)
        }
    }
}
