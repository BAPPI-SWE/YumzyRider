package com.yumzy.rider

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp

// Data class to hold all necessary order details for an active delivery
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
fun ActiveDeliveryScreen(
    order: Order,
    onStatusUpdate: (orderId: String, newStatus: String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Active Delivery", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Divider()

            // Pickup Info
            InfoRow(
                icon = Icons.Default.Storefront,
                title = "PICKUP FROM",
                primaryText = order.restaurantName
            )

            // Delivery Info
            InfoRow(
                icon = Icons.Default.Home,
                title = "DELIVER TO",
                primaryText = order.userName,
                secondaryText = order.fullAddress
            )

            // Contact Info
            InfoRow(
                icon = Icons.Default.Phone,
                title = "CONTACT",
                primaryText = order.userPhone
            )

            Divider()

            // Items Summary
            Text("Items:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                order.items.forEach { item ->
                    Text("• ${item["quantity"]} x ${item["itemName"]}")
                }
            }

            Divider()

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total to Collect:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("৳${order.totalPrice}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { onStatusUpdate(order.id, "On the way") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Picked Up")
                }
                Button(
                    onClick = { onStatusUpdate(order.id, "Delivered") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Delivered")
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, title: String, primaryText: String, secondaryText: String? = null) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.padding(top = 4.dp).size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
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