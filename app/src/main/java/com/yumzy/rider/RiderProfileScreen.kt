package com.yumzy.rider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderProfileScreen(
    onSaveProfile: (phone: String, vehicle: String, servesDaffodil: Boolean, servesNsu: Boolean) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var vehicle by remember { mutableStateOf("") }
    var servesDaffodil by remember { mutableStateOf(true) }
    var servesNsu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Complete Your Rider Profile") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = vehicle,
                onValueChange = { vehicle = it },
                label = { Text("Vehicle Details (e.g., Motorcycle, Bicycle)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Text("Serviceable Locations", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text("Select where you can make deliveries.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = servesDaffodil, onCheckedChange = { servesDaffodil = it })
                Text("Daffodil Smart City")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = servesNsu, onCheckedChange = { servesNsu = it })
                Text("North South University")
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { onSaveProfile(phone, vehicle, servesDaffodil, servesNsu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Delivering", fontSize = 16.sp)
            }
        }
    }
}