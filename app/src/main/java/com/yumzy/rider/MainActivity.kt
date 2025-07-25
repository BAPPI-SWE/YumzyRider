package com.yumzy.rider

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yumzy.rider.auth.AuthScreen
import com.yumzy.rider.auth.AuthViewModel
import com.yumzy.rider.auth.GoogleAuthUiClient
import com.yumzy.rider.features.profile.RiderAccountScreen
import com.yumzy.rider.features.profile.RiderEditProfileScreen
import com.yumzy.rider.navigation.MainScreen
import com.yumzy.rider.ui.theme.YumzyRiderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YumzyRiderTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "auth") {
                    composable("auth") {
                        val viewModel = viewModel<AuthViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()

                        LaunchedEffect(key1 = Unit) {
                            val currentUser = googleAuthUiClient.getSignedInUser()
                            if (currentUser != null) {
                                checkRiderProfile(currentUser.userId, navController)
                            }
                        }

                        val launcher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartIntentSenderForResult()
                        ) { result ->
                            if (result.resultCode == RESULT_OK) {
                                lifecycleScope.launch {
                                    val signInResult = googleAuthUiClient.signInWithIntent(
                                        intent = result.data ?: return@launch
                                    )
                                    viewModel.onSignInResult(signInResult)
                                }
                            }
                        }

                        LaunchedEffect(key1 = state.isSignInSuccessful) {
                            if (state.isSignInSuccessful) {
                                val userId = googleAuthUiClient.getSignedInUser()?.userId
                                if (userId != null) {
                                    checkRiderProfile(userId, navController)
                                }
                                viewModel.resetState()
                            }
                        }

                        AuthScreen(
                            onSignInClick = {
                                lifecycleScope.launch {
                                    val signInIntentSender = googleAuthUiClient.signIn()
                                    launcher.launch(
                                        IntentSenderRequest.Builder(
                                            signInIntentSender ?: return@launch
                                        ).build()
                                    )
                                }
                            }
                        )
                    }
                    composable("create_profile") {
                        RiderProfileScreen(
                            onSaveProfile = { phone, vehicle, servesDaffodil, servesNsu ->
                                val userId = Firebase.auth.currentUser?.uid ?: return@RiderProfileScreen
                                val serviceableLocations = mutableListOf<String>()
                                if (servesDaffodil) serviceableLocations.add("Daffodil Smart City")
                                if (servesNsu) serviceableLocations.add("North South University")

                                val riderProfile = hashMapOf(
                                    "name" to (Firebase.auth.currentUser?.displayName ?: "N/A"),
                                    "phone" to phone,
                                    "vehicle" to vehicle,
                                    "serviceableLocations" to serviceableLocations,
                                    "isAvailable" to false,
                                    "uid" to userId
                                )
                                Firebase.firestore.collection("riders").document(userId)
                                    .set(riderProfile)
                                    .addOnSuccessListener {
                                        navController.navigate("main") { popUpTo("auth") { inclusive = true } }
                                    }
                            }
                        )
                    }
                    composable("main") {
                        MainScreen(
                            onAcceptOrder = { orderId ->
                                val riderId = Firebase.auth.currentUser?.uid
                                val riderName = Firebase.auth.currentUser?.displayName
                                if (riderId != null) {
                                    Firebase.firestore.collection("orders").document(orderId)
                                        .update(mapOf(
                                            "orderStatus" to "Accepted",
                                            "riderId" to riderId,
                                            "riderName" to riderName
                                        ))
                                        .addOnSuccessListener {
                                            Toast.makeText(applicationContext, "Order Accepted!", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                            onUpdateOrderStatus = { orderId, newStatus ->
                                Firebase.firestore.collection("orders").document(orderId)
                                    .update("orderStatus", newStatus)
                                    .addOnSuccessListener {
                                        Toast.makeText(applicationContext, "Order marked as $newStatus", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            onSignOut = {
                                lifecycleScope.launch {
                                    googleAuthUiClient.signOut()
                                    navController.navigate("auth") {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            },
                            onNavigateToEditProfile = {
                                navController.navigate("edit_profile")
                            }
                        )
                    }
                    composable("edit_profile") {
                        RiderEditProfileScreen(
                            onBackClicked = { navController.popBackStack() },
                            onSaveChanges = { phone, vehicle, serviceableLocations ->
                                val userId = Firebase.auth.currentUser?.uid ?: return@RiderEditProfileScreen
                                val updates = mapOf(
                                    "phone" to phone,
                                    "vehicle" to vehicle,
                                    "serviceableLocations" to serviceableLocations
                                )
                                Firebase.firestore.collection("riders").document(userId)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        Toast.makeText(applicationContext, "Profile Updated", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                            }
                        )
                    }
                    composable("account") {
                        RiderAccountScreen(
                            onNavigateToEditProfile = { navController.navigate("edit_profile") },
                            onBackClicked = { navController.popBackStack() },
                            onSignOut = {
                                lifecycleScope.launch {
                                    googleAuthUiClient.signOut()
                                    navController.navigate("auth") {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkRiderProfile(userId: String, navController: NavController) {
        Firebase.firestore.collection("riders").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    navController.navigate("main") { popUpTo("auth") { inclusive = true } }
                } else {
                    navController.navigate("create_profile") { popUpTo("auth") { inclusive = true } }
                }
            }
    }
}