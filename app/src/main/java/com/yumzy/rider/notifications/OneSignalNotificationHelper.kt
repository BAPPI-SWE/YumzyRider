package com.yumzy.rider.notifications

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object OneSignalNotificationHelper {
    private const val ONE_SIGNAL_APP_ID = "dabb9362-80ed-4e54-be89-32ffc7dbf383"
    // Use your REST API Key from OneSignal dashboard (Settings → Keys & IDs)
    private const val ONE_SIGNAL_REST_API_KEY = "os_v2_app_3k5zgyua5vhfjpujgl74pw7tqonyuq6vbuhuicmwd4v5m3xf3nt32dn5kxntcjqns5a562jfe7f52bl62ttrnhwbledfbnh6wl6c5tq"
    private const val ONE_SIGNAL_API_URL = "https://onesignal.com/api/v1/notifications"  // Keep v1
    private val client = OkHttpClient()

    suspend fun sendOrderStatusNotification(userId: String, orderId: String, newStatus: String, restaurantName: String) {
        Log.d("OneSignal", "Starting notification process for userId: $userId, orderId: $orderId, status: $newStatus")

        try {
            Log.d("OneSignal", "Fetching playerId for userId: $userId")
            val userDoc = Firebase.firestore.collection("users").document(userId)
                .get().await()
            val playerId = userDoc.getString("oneSignalPlayerId")
            Log.d("OneSignal", "Fetched playerId: $playerId")

            if (playerId == null) {
                Log.w("OneSignal", "No playerId found for userId: $userId")
                return
            }

            Log.d("OneSignal", "Creating notification payload")
            val payload = JSONObject().apply {
                put("app_id", ONE_SIGNAL_APP_ID)
                put("include_player_ids", JSONArray().put(playerId))
                put("headings", JSONObject().put("en", "Order Status Update"))
                put("contents", JSONObject().put("en", "Your order from $restaurantName (ID: $orderId) is now $newStatus."))
                put("data", JSONObject().put("orderId", orderId))
            }
            Log.d("OneSignal", "Payload created: $payload")

            Log.d("OneSignal", "Sending HTTP request to OneSignal API")
            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            Log.d("OneSignal", "Request Headers: Authorization=Basic $ONE_SIGNAL_REST_API_KEY")
            val request = Request.Builder()
                .url(ONE_SIGNAL_API_URL)
                .addHeader("Authorization", "Basic $ONE_SIGNAL_REST_API_KEY") // ✅ fixed
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            withContext(Dispatchers.IO) {
                Log.d("OneSignal", "Executing network call")
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: "No response body"
                    Log.d("OneSignal", "API Response: Code ${response.code}, Body $responseBody")
                    if (response.isSuccessful) {
                        Log.d("OneSignal", "Notification sent successfully for order $orderId: $responseBody")
                    } else {
                        Log.e("OneSignal", "Failed to send notification for order $orderId: Code ${response.code}, Body $responseBody")
                    }
                }
            }
            Log.d("OneSignal", "Notification process completed for order $orderId")
        } catch (e: Exception) {
            Log.e("OneSignal", "Error in notification process for order $orderId: ${e.message}", e)
        }
    }
}
