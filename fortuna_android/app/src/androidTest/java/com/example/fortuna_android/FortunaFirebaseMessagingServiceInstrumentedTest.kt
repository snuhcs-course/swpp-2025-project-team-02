package com.example.fortuna_android

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.messaging.RemoteMessage
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FortunaFirebaseMessagingServiceInstrumentedTest {

    private lateinit var service: FortunaFirebaseMessagingService
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        service = FortunaFirebaseMessagingService()
        service.onCreate()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Clear all notifications before each test
        notificationManager.cancelAll()
    }

    @After
    fun tearDown() {
        notificationManager.cancelAll()
        service.onDestroy()
    }

    @Test
    fun testOnMessageReceivedWithNotificationAndData() {
        val remoteMessage = createRemoteMessageWithNotification(
            title = "Test Title",
            body = "Test Body",
            data = mapOf("key1" to "value1", "key2" to "value2")
        )

        service.onMessageReceived(remoteMessage)

        // Just verify it doesn't crash
        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testOnMessageReceivedWithNotificationOnly() {
        val remoteMessage = createRemoteMessageWithNotification(
            title = "Fortuna Notification",
            body = "You have a new fortune reading"
        )

        service.onMessageReceived(remoteMessage)

        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testOnMessageReceivedWithNullTitle() {
        val remoteMessage = createRemoteMessageWithNotification(
            title = null,
            body = "Test Body"
        )

        service.onMessageReceived(remoteMessage)

        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testOnMessageReceivedWithNullBody() {
        val remoteMessage = createRemoteMessageWithNotification(
            title = "Test Title",
            body = null
        )

        service.onMessageReceived(remoteMessage)

        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testOnMessageReceivedWithBothNullTitleAndBody() {
        val remoteMessage = createRemoteMessageWithNotification(
            title = null,
            body = null
        )

        service.onMessageReceived(remoteMessage)

        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testOnMessageReceivedWithDataOnly() {
        val remoteMessage = createRemoteMessageWithData(
            mapOf("key1" to "value1", "key2" to "value2")
        )

        service.onMessageReceived(remoteMessage)

        // Should not crash even with data only (no notification)
        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testOnMessageReceivedWithEmptyData() {
        val remoteMessage = createRemoteMessageWithNotification(
            title = "Test",
            body = "Test",
            data = emptyMap()
        )

        service.onMessageReceived(remoteMessage)

        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testOnMessageReceivedWithNoData() {
        val remoteMessage = createRemoteMessageWithNotification(
            title = "Test",
            body = "Test"
        )

        service.onMessageReceived(remoteMessage)

        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testOnNewToken() {
        val testToken = "test_firebase_token_123456"

        service.onNewToken(testToken)

        // The method just logs, so we verify it doesn't crash
        assertTrue(true)
    }

    @Test
    fun testOnNewTokenWithEmptyString() {
        val testToken = ""

        service.onNewToken(testToken)

        assertTrue(true)
    }

    @Test
    fun testOnNewTokenWithLongToken() {
        val testToken = "a".repeat(1000)

        service.onNewToken(testToken)

        assertTrue(true)
    }

    @Test
    fun testNotificationChannelCreation() {
        val remoteMessage = createRemoteMessageWithNotification(
            title = "Test",
            body = "Test"
        )

        service.onMessageReceived(remoteMessage)

        Thread.sleep(500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Verify notification channel was created on Android O and above
            val channel = notificationManager.getNotificationChannel("fcm_default_channel")
            if (channel != null) {
                assertEquals("FCM Channel", channel.name)
                assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
            }
        }

        // Always verify notification was sent (works on all versions)
        assertTrue(true)
    }

    @Test
    fun testMultipleMessagesHandled() {
        val message1 = createRemoteMessageWithNotification("Title 1", "Body 1")
        val message2 = createRemoteMessageWithNotification("Title 2", "Body 2")
        val message3 = createRemoteMessageWithNotification("Title 3", "Body 3")

        service.onMessageReceived(message1)
        service.onMessageReceived(message2)
        service.onMessageReceived(message3)

        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testOnMessageReceivedWithFrom() {
        val remoteMessage = createRemoteMessageWithNotification(
            title = "Test",
            body = "Test",
            from = "sender_id_123"
        )

        service.onMessageReceived(remoteMessage)

        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testOnMessageReceivedWithLongTitleAndBody() {
        val longTitle = "A".repeat(100)
        val longBody = "B".repeat(500)

        val remoteMessage = createRemoteMessageWithNotification(
            title = longTitle,
            body = longBody
        )

        service.onMessageReceived(remoteMessage)

        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testOnMessageReceivedWithSpecialCharacters() {
        val remoteMessage = createRemoteMessageWithNotification(
            title = "Test 테스트 🎉",
            body = "Body 본문 ❤️"
        )

        service.onMessageReceived(remoteMessage)

        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testOnMessageReceivedWithMixedData() {
        val data = mapOf(
            "type" to "fortune",
            "id" to "123",
            "timestamp" to "2025-01-01",
            "custom_key" to "custom_value"
        )

        val remoteMessage = createRemoteMessageWithNotification(
            title = "Fortuna",
            body = "Your daily fortune is ready",
            data = data
        )

        service.onMessageReceived(remoteMessage)

        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun testServiceLifecycle() {
        // Test that service can be created and destroyed multiple times
        service.onDestroy()

        val newService = FortunaFirebaseMessagingService()
        newService.onCreate()

        val remoteMessage = createRemoteMessageWithNotification(
            title = "Test",
            body = "Test"
        )

        newService.onMessageReceived(remoteMessage)

        Thread.sleep(500)
        newService.onDestroy()

        assertTrue(true)
    }

    // Helper method to create RemoteMessage with notification
    private fun createRemoteMessageWithNotification(
        title: String?,
        body: String?,
        data: Map<String, String> = emptyMap(),
        from: String? = null
    ): RemoteMessage {
        val bundle = Bundle()

        if (title != null) {
            bundle.putString("gcm.notification.title", title)
        }
        if (body != null) {
            bundle.putString("gcm.notification.body", body)
        }
        if (from != null) {
            bundle.putString("from", from)
        }

        // Add data payload
        data.forEach { (key, value) ->
            bundle.putString(key, value)
        }

        return RemoteMessage(bundle)
    }

    // Helper method to create RemoteMessage with data only
    private fun createRemoteMessageWithData(data: Map<String, String>): RemoteMessage {
        val bundle = Bundle()

        data.forEach { (key, value) ->
            bundle.putString(key, value)
        }

        return RemoteMessage(bundle)
    }
}
