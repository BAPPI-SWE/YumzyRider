package com.yumzy.rider.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.rider.ActiveDeliveryScreen
import com.yumzy.rider.Order

@Composable
fun MyDeliveriesScreen(onUpdateOrderStatus: (orderId: String, newStatus: String) -> Unit) {
    var activeOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = Unit) {
        val riderId = Firebase.auth.currentUser?.uid ?: return@LaunchedEffect
        Firebase.firestore.collection("orders")
            .whereEqualTo("riderId", riderId)
            .whereIn("orderStatus", listOf("Accepted", "On the way"))
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                snapshot?.let {
                    activeOrders = it.documents.mapNotNull { doc ->
                        val address = "Building: ${doc.getString("building")}, Floor: ${doc.getString("floor")}, Room: ${doc.getString("room")}\n${doc.getString("userSubLocation")}, ${doc.getString("userBaseLocation")}"
                        doc.toObject(Order::class.java)?.copy(id = doc.id, fullAddress = address)
                    }
                }
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("My Active Deliveries", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (isLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (activeOrders.isEmpty()) {
            Text("You have no active deliveries.", color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(activeOrders) { order ->
                    ActiveDeliveryScreen(order = order, onStatusUpdate = onUpdateOrderStatus)
                }
            }
        }
    }
}