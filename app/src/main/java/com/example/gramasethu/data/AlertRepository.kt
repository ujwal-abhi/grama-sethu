package com.example.gramasethu.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class AlertData(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "INFO",
    val timestamp: Long = 0,
    val isCustom: Boolean = false,
    val location: String = "",
    val severity: String = ""
)

class AlertRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getAlerts(onResult: (List<AlertData>) -> Unit) {
        val combined = mutableListOf<AlertData>()
        var firestoreAlertsLoaded = false
        var customReportsLoaded = false

        fun tryEmit() {
            if (firestoreAlertsLoaded && customReportsLoaded) {
                val sorted = combined.sortedByDescending { it.timestamp }
                onResult(sorted)
            }
        }

        // Load official alerts
        db.collection("alerts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                combined.removeAll { !it.isCustom }
                val alerts = snapshot?.documents?.map { doc ->
                    AlertData(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        type = doc.getString("type") ?: "INFO",
                        timestamp = doc.getLong("timestamp") ?: 0,
                        isCustom = false
                    )
                } ?: emptyList()
                combined.addAll(alerts)
                firestoreAlertsLoaded = true
                tryEmit()
            }

        // Load custom reports
        db.collection("custom_reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                combined.removeAll { it.isCustom }
                val reports = snapshot?.documents?.map { doc ->
                    val reportType = doc.getString("type") ?: "INFO"
                    val severity = doc.getString("severity") ?: "Low"
                    val location = doc.getString("location") ?: ""
                    val description = doc.getString("description") ?: ""
                    val alertType = when (severity) {
                        "High"   -> "DANGER"
                        "Medium" -> "WARNING"
                        else     -> "INFO"
                    }
                    val title = when (reportType) {
                        "FLOOD"  -> "🌊 Flood reported"
                        "BRIDGE" -> "🌉 Unknown bridge issue"
                        "ROAD"   -> "🚧 Road blocked"
                        "DANGER" -> "⚠️ Danger zone reported"
                        else     -> "📋 Community report"
                    }
                    AlertData(
                        id = doc.id,
                        title = title,
                        message = "$description\n📍 $location",
                        type = alertType,
                        timestamp = doc.getLong("timestamp") ?: 0,
                        isCustom = true,
                        location = location,
                        severity = severity
                    )
                } ?: emptyList()
                combined.addAll(reports)
                customReportsLoaded = true
                tryEmit()
            }
    }
}