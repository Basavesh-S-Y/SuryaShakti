package com.suryashakti.solarmonitor.util

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.suryashakti.solarmonitor.data.EnergyLog
import com.suryashakti.solarmonitor.data.WeatherCondition
import kotlinx.coroutines.tasks.await

data class SyncResult(val uploaded: Int, val downloaded: Int)

object CloudSyncManager {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Bidirectional sync:
     *  1. UPLOAD  — push every local EnergyLog to Firestore (upsert by dateMillis)
     *  2. DOWNLOAD — pull any remote logs not present locally
     *
     * Returns a [SyncResult] describing how many records moved each direction.
     */
    suspend fun sync(
        uid: String,
        localLogs: List<EnergyLog>,
        onSaveLocal: suspend (EnergyLog) -> Unit
    ): SyncResult {
        val collection = db.collection("users").document(uid).collection("energy_logs")

        // ── 1. UPLOAD local → cloud ────────────────────────────────────────
        var uploaded = 0
        for (log in localLogs) {
            val docId = log.dateMillis.toString()
            collection.document(docId)
                .set(log.toFirestoreMap(), SetOptions.merge())
                .await()
            uploaded++
        }

        // ── 2. DOWNLOAD cloud → local ──────────────────────────────────────
        var downloaded = 0
        val localDates = localLogs.map { it.dateMillis }.toSet()
        val snapshot = collection.get().await()

        for (doc in snapshot.documents) {
            val dateMillis = doc.id.toLongOrNull() ?: continue
            if (dateMillis in localDates) continue   // already have it locally
            val log = doc.toEnergyLog() ?: continue
            onSaveLocal(log)
            downloaded++
        }

        // ── 3. Update user last-sync timestamp ────────────────────────────
        db.collection("users").document(uid)
            .set(mapOf("lastSyncAt" to System.currentTimeMillis()), SetOptions.merge())
            .await()

        return SyncResult(uploaded, downloaded)
    }

    // ── Serialisation helpers ─────────────────────────────────────────────

    private fun EnergyLog.toFirestoreMap(): Map<String, Any> = mapOf(
        "id"               to id,
        "dateMillis"       to dateMillis,
        "generatedKwh"     to generatedKwh,
        "consumedKwh"      to consumedKwh,
        "weatherCondition" to weatherCondition.name,
        "perUnitRate"      to perUnitRate,
        "exportRate"       to exportRate,
        "panelCapacityKw"  to panelCapacityKw,
        "notes"            to notes,
        "syncedAt"         to System.currentTimeMillis()
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toEnergyLog(): EnergyLog? {
        return try {
            EnergyLog(
                id               = getLong("id") ?: 0L,
                dateMillis       = getLong("dateMillis") ?: return null,
                generatedKwh     = getDouble("generatedKwh") ?: 0.0,
                consumedKwh      = getDouble("consumedKwh") ?: 0.0,
                weatherCondition = WeatherCondition.valueOf(
                    getString("weatherCondition") ?: WeatherCondition.SUNNY.name
                ),
                perUnitRate      = getDouble("perUnitRate") ?: 8.0,
                exportRate       = getDouble("exportRate") ?: 4.0,
                panelCapacityKw  = getDouble("panelCapacityKw") ?: 3.0,
                notes            = getString("notes") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Delete a single log from Firestore (called when user deletes locally). */
    suspend fun deleteLog(uid: String, dateMillis: Long) {
        db.collection("users").document(uid)
            .collection("energy_logs")
            .document(dateMillis.toString())
            .delete().await()
    }

    /** Save user profile metadata to Firestore. */
    suspend fun saveUserProfile(uid: String, name: String, email: String) {
        db.collection("users").document(uid).set(
            mapOf(
                "displayName" to name,
                "email"       to email,
                "updatedAt"   to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()
    }
}
