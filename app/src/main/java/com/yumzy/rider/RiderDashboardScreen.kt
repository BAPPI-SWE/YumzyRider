package com.yumzy.rider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class RiderProfile(
    val name: String = "",
    val serviceableLocations: List<String> = emptyList()
)

data class OrderRequest(
    val id: String = "",
    val restaurantName: String = "",
    val userSubLocation: String = "",
    val totalPrice: Double = 0.0
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

                // Only start listening for orders if the rider has serviceable locations
                if (profile.serviceableLocations.isNotEmpty()) {
                    db.collection("orders")
                        .whereEqualTo("orderStatus", "Pending")
                        .whereEqualTo("orderType", "Instant")
                        .whereIn("userBaseLocation", profile.serviceableLocations)
                        .addSnapshotListener { snapshot, _ ->
                            snapshot?.let {
                                availableOrders = it.documents.mapNotNull { orderDoc ->
                                    OrderRequest(
                                        id = orderDoc.id,
                                        restaurantName = orderDoc.getString("restaurantName") ?: "N/A",
                                        userSubLocation = orderDoc.getString("userSubLocation") ?: "N/A",
                                        totalPrice = orderDoc.getDouble("totalPrice") ?: 0.0
                                    )
                                }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Welcome, ${riderProfile?.name ?: "..."}")
                        Text(
                            text = if (isAvailable) "You are Online" else "You are Offline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAvailable) Color(0xFF2E7D32) else Color.Gray
                        )
                    }
                    if(isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Switch(
                        checked = isAvailable,
                        onCheckedChange = { updateAvailability(it) },
                        modifier = Modifier.scale(1.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Available Deliveries", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                // Don't show anything while the initial profile loads
            } else if (!isAvailable) {
                Text("You are offline. Go online to see new orders.", color = Color.Gray)
            } else if (availableOrders.isEmpty()) {
                Text("No new orders right now. We'll notify you!", color = Color.Gray)
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

@Composable
fun OrderRequestCard(order: OrderRequest, onAccept: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(order.restaurantName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Deliver to: ${order.userSubLocation}")
            Text("Order Total: ৳${order.totalPrice}")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
                Text("Accept Order")
            }
        }
    }
}