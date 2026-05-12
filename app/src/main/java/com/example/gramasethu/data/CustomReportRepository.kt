package com.example.gramasethu.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class CustomReportMarker(
    val id: String = "",
    val type: String = "",
    val severity: String = "",
    val location: String = "",
    val description: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestamp: Long = 0
)

class CustomReportRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getCustomReports(onResult: (List<CustomReportMarker>) -> Unit) {
        db.collection("custom_reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CustomReportRepo", "Error: ${error.message}")
                    return@addSnapshotListener
                }
                val reports = snapshot?.documents?.mapNotNull { doc ->
                    val lat = doc.getDouble("lat") ?: 0.0
                    val lng = doc.getDouble("lng") ?: 0.0
                    android.util.Log.d("CustomReportRepo", "Report: $lat, $lng")
                    // Show all reports that have valid coordinates
                    if (lat == 0.0 || lng == 0.0) {
                        android.util.Log.d("CustomReportRepo", "Skipping report with no coords")
                        return@mapNotNull null
                    }
                    CustomReportMarker(
                        id = doc.id,
                        type = doc.getString("type") ?: "",
                        severity = doc.getString("severity") ?: "",
                        location = doc.getString("location") ?: "",
                        description = doc.getString("description") ?: "",
                        lat = lat,
                        lng = lng,
                        timestamp = doc.getLong("timestamp") ?: 0
                    )
                } ?: emptyList()
                android.util.Log.d("CustomReportRepo", "Total reports with coords: ${reports.size}")
                onResult(reports)
            }
    }
}