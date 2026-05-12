package com.example.gramasethu.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReportRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun submitReport(
        bridgeId: String,
        bridgeName: String,
        status: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val report = hashMapOf(
            "bridgeId" to bridgeId,
            "bridgeName" to bridgeName,
            "userId" to (auth.currentUser?.uid ?: "anonymous"),
            "status" to status,
            "timestamp" to System.currentTimeMillis()
        )

        // Step 1 — save the report
        db.collection("reports")
            .add(report)
            .addOnSuccessListener {
                // Step 2 — check how many reports for this status
                checkAndUpdateBridgeStatus(bridgeId, status, onSuccess, onFailure)
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Failed to submit report")
            }
    }

    private fun checkAndUpdateBridgeStatus(
        bridgeId: String,
        status: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("reports")
            .whereEqualTo("bridgeId", bridgeId)
            .whereEqualTo("status", status)
            .get()
            .addOnSuccessListener { result ->
                // If 3 or more people report same status → update bridge
                if (result.size() >= 3) {
                    db.collection("bridges")
                        .document(bridgeId)
                        .update("status", status)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onFailure(e.message ?: "Update failed") }
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Check failed")
            }
    }
}