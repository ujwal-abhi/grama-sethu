package com.example.gramasethu.data

import com.example.gramasethu.ui.screens.BridgeUi
import com.google.firebase.firestore.FirebaseFirestore

class BridgeRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getBridges(onResult: (List<BridgeUi>) -> Unit) {
        db.collection("bridges")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val bridges = snapshot?.documents?.map { doc ->
                    BridgeUi(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        status = doc.getString("status") ?: "OPEN",
                        lat = doc.getDouble("lat") ?: 0.0,
                        lng = doc.getDouble("lng") ?: 0.0
                    )
                } ?: emptyList()
                onResult(bridges)
            }
    }
}