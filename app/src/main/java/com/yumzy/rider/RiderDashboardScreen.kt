package com.yumzy.rider

import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderDashboardScreen() {
    var isAvailable by remember { mutableStateOf(false) }
    var riderName by remember { mutableStateOf("...") }
    var isLoading by remember { mutableStateOf(true) }
    val riderId = Firebase.auth.currentUser?.uid

    LaunchedEffect(key1 = riderId) {
        if (riderId != null) {
            val riderDocRef = Firebase.firestore.collection("riders").document(riderId)
            riderDocRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    isAvailable = snapshot.getBoolean("isAvailable") ?: false
                    riderName = snapshot.getString("name") ?: "Rider"
                    isLoading = false
                }
            }
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
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Welcome, $riderName!", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Your Status",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = if (isAvailable) "Online" else "Offline",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isAvailable) Color(0xFF2E7D32) else Color.Gray
                            )
                        }
                        Switch(
                            checked = isAvailable,
                            onCheckedChange = { newStatus ->
                                isAvailable = newStatus
                                updateAvailability(newStatus)
                            },
                            modifier = Modifier.scale(1.5f)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Toggle the switch to go online and receive delivery requests.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}