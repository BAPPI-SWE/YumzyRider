package com.yumzy.rider.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

data class Order(
    val id: String = "",
    val restaurantName: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val fullAddress: String = "",
    val totalPrice: Double = 0.0,
    val items: List<Map<String, Any>> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)

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
                    }.sortedBy { it.createdAt }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                    ActiveDeliveryCard(order = order, onStatusUpdate = onUpdateOrderStatus)
                }
            }
        }
    }
}

@Composable
fun ActiveDeliveryCard(order: Order, onStatusUpdate: (orderId: String, newStatus: String) -> Unit) {
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ongoing Delivery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(sdf.format(order.createdAt.toDate()), style = MaterialTheme.typography.bodySmall)
            }

            InfoRow(
                icon = Icons.Default.Storefront,
                title = "PICKUP FROM",
                primaryText = order.restaurantName
            )
            InfoRow(
                icon = Icons.Default.Home,
                title = "DELIVER TO",
                primaryText = order.userName,
                secondaryText = order.fullAddress
            )
            InfoRow(
                icon = Icons.Default.Phone,
                title = "CONTACT",
                primaryText = order.userPhone
            )
            Divider()
            Text("Items:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                order.items.forEach { item ->
                    Text("• ${item["quantity"]} x ${item["itemName"]}")
                }
            }
            Divider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Order Total:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("৳${order.totalPrice}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(onClick = { onStatusUpdate(order.id, "On the way") }, modifier = Modifier.weight(1f)) {
                    Text("Picked Up")
                }
                Button(onClick = { onStatusUpdate(order.id, "Delivered") }, modifier = Modifier.weight(1f)) {
                    Text("Delivered")
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, title: String, primaryText: String, secondaryText: String? = null) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = title, modifier = Modifier
            .padding(top = 4.dp)
            .size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(primaryText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (secondaryText != null) {
                Text(secondaryText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}