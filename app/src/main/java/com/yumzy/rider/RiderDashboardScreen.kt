package com.yumzy.rider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

data class RiderProfile(
    val name: String = "",
    val serviceableLocations: List<String> = emptyList()
)

// UPDATED: OrderRequest now holds the createdAt timestamp
data class OrderRequest(
    val id: String = "",
    val restaurantName: String = "",
    val totalPrice: Double = 0.0,
    val items: List<Map<String, Any>> = emptyList(),
    val fullAddress: String = "",
    val userPhone: String = "",
    val userBaseLocation: String = "",
    val createdAt: Timestamp = Timestamp.now() // New field
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderDashboardScreen(
    onAcceptOrder: (orderId: String) -> Unit
) {
    var riderProfile by remember { mutableStateOf<RiderProfile?>(null) }
    var isAvailable by remember { mutableStateOf(false) }
    var availableOrders by remember { mutableStateOf<List<OrderRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val riderId = Firebase.auth.currentUser?.uid

    LaunchedEffect(key1 = riderId) {
        if (riderId == null) return@LaunchedEffect

        val db = Firebase.firestore
        val riderDocRef = db.collection("riders").document(riderId)

        riderDocRef.get().addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                val profile = RiderProfile(
                    name = doc.getString("name") ?: "Rider",
                    serviceableLocations = doc.get("serviceableLocations") as? List<String> ?: emptyList()
                )
                riderProfile = profile

                if (profile.serviceableLocations.isNotEmpty()) {
                    db.collection("orders")
                        .whereEqualTo("orderStatus", "Pending")
                        .whereEqualTo("orderType", "Instant")
                        .whereIn("userBaseLocation", profile.serviceableLocations)
                        .addSnapshotListener { snapshot, _ ->
                            snapshot?.let {
                                availableOrders = it.documents.mapNotNull { orderDoc ->
                                    val address = "Building: ${orderDoc.getString("building")}, Floor: ${orderDoc.getString("floor")}, Room: ${orderDoc.getString("room")}\n${orderDoc.getString("userSubLocation")}, ${orderDoc.getString("userBaseLocation")}"
                                    OrderRequest(
                                        id = orderDoc.id,
                                        restaurantName = orderDoc.getString("restaurantName") ?: "N/A",
                                        totalPrice = orderDoc.getDouble("totalPrice") ?: 0.0,
                                        items = orderDoc.get("items") as? List<Map<String, Any>> ?: emptyList(),
                                        fullAddress = address,
                                        userPhone = orderDoc.getString("userPhone") ?: "Not provided",
                                        userBaseLocation = orderDoc.getString("userBaseLocation") ?: "",
                                        createdAt = orderDoc.getTimestamp("createdAt") ?: Timestamp.now() // Fetch the timestamp
                                    )
                                }.sortedByDescending { it.createdAt } // Show newest orders first
                            }
                        }
                }
            }
        }

        riderDocRef.addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                isAvailable = it.getBoolean("isAvailable") ?: false
            }
            isLoading = false
        }
    }

    fun updateAvailability(newStatus: Boolean) {
        if (riderId != null) {
            Firebase.firestore.collection("riders").document(riderId)
                .update("isAvailable", newStatus)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Rider Dashboard") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Welcome, ${riderProfile?.name ?: "..."}")
                        Text(if (isAvailable) "You are Online" else "You are Offline",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = if (isAvailable) Color(0xFF2E7D32) else Color.Gray)
                    }
                    if(isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Switch(checked = isAvailable, onCheckedChange = { updateAvailability(it) }, modifier = Modifier.scale(1.2f))
                }
            }

            Text("Available Deliveries", style = MaterialTheme.typography.headlineSmall)

            if (!isAvailable) {
                Text("You are offline. Go online to see new orders.", color = Color.Gray)
            } else if (availableOrders.isEmpty()) {
                Text("No new orders right now.", color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(availableOrders) { order ->
                        OrderRequestCard(
                            order = order,
                            onAccept = { onAcceptOrder(order.id) }
                        )
                    }
                }
            }
        }
    }
}

// UPDATED: The OrderRequestCard now shows the time
@Composable
fun OrderRequestCard(order: OrderRequest, onAccept: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(order.restaurantName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                // Display the formatted time
                Text(formatDate(order.createdAt), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = "Phone", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(order.userPhone)
            }
            Text("Deliver to:", fontWeight = FontWeight.SemiBold)
            Text(order.fullAddress)
            Divider()
            Text("Items:", fontWeight = FontWeight.SemiBold)
            order.items.forEach { item ->
                Text("- ${item["quantity"]} x ${item["itemName"]}")
            }
            Divider()
            Text("Order Total: ৳${order.totalPrice}", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
                Text("Accept Order")
            }
        }
    }
}

// NEW: Helper function to format the timestamp
fun formatDate(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}