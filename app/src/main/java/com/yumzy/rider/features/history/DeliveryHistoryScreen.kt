package com.yumzy.rider.features.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.rider.main.Order // Re-using the Order data class from MyDeliveriesScreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryHistoryScreen(onBackClicked: () -> Unit) {
    var completedOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // This effect fetches the delivered orders for the current rider from Firestore
    LaunchedEffect(key1 = Unit) {
        val riderId = Firebase.auth.currentUser?.uid ?: return@LaunchedEffect
        Firebase.firestore.collection("orders")
            .whereEqualTo("riderId", riderId)
            .whereEqualTo("orderStatus", "Delivered")
            .orderBy("createdAt", Query.Direction.DESCENDING) // Show most recent orders first
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null) {
                    // Handle potential errors, e.g., logging
                    return@addSnapshotListener
                }
                snapshot?.let {
                    completedOrders = it.documents.mapNotNull { doc ->
                        val address = "Building: ${doc.getString("building")}, Floor: ${doc.getString("floor")}, Room: ${doc.getString("room")}\n${doc.getString("userSubLocation")}, ${doc.getString("userBaseLocation")}"
                        doc.toObject(Order::class.java)?.copy(id = doc.id, fullAddress = address)
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery History") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (completedOrders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You have no completed deliveries yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(completedOrders) { order ->
                        CompletedDeliveryCard(order = order)
                    }
                }
            }
        }
    }
}

/**
 * A card composable to display information about a single completed delivery.
 */
@Composable
fun CompletedDeliveryCard(order: Order) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    order.restaurantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "৳${order.totalPrice}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Delivered to: ${order.userName}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Address: ${order.fullAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Order ID: ${order.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    sdf.format(order.createdAt.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
