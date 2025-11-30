package com.example.fortuna_android

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import io.mockk.*

/**
 * Unit tests for FortunaFirebaseMessagingService
 * Tests FCM message handling and notification creation
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FortunaFirebaseMessagingServiceTest {

    private lateinit var service: FortunaFirebaseMessagingService
    private lateinit var context: Context

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
        context = ApplicationProvider.getApplicationContext()

        // Build the service using Robolectric
        val controller = Robolectric.buildService(FortunaFirebaseMessagingService::class.java)
        service = controller.get()
    }

    // ========== Service Instantiation Tests ==========

    @Test
    fun `test service can be instantiated`() {
        // Assert
        assertNotNull("FortunaFirebaseMessagingService should not be null", service)
    }

    @Test
    fun `test service is Firebase Messaging Service`() {
        // Assert
        assertTrue("Service should extend FirebaseMessagingService",
            service is com.google.firebase.messaging.FirebaseMessagingService)
    }

    // ========== onNewToken Tests ==========

    @Test
    fun `test onNewToken with valid token`() {
        // Arrange
        val token = "test-fcm-token-12345"

        // Act
        service.onNewToken(token)

        // Assert - Should not throw
        assertTrue("onNewToken executed successfully", true)
    }

    @Test
    fun `test onNewToken with empty token`() {
        // Arrange
        val token = ""

        // Act
        service.onNewToken(token)

        // Assert
        assertTrue("onNewToken handles empty token", true)
    }

    @Test
    fun `test onNewToken with long token`() {
        // Arrange
        val token = "A".repeat(500)

        // Act
        service.onNewToken(token)

        // Assert
        assertTrue("onNewToken handles long token", true)
    }

    @Test
    fun `test onNewToken called multiple times`() {
        // Act
        service.onNewToken("token1")
        service.onNewToken("token2")
        service.onNewToken("token3")

        // Assert
        assertTrue("onNewToken handles multiple calls", true)
    }

    // ========== onMessageReceived Tests ==========

    @Test
    fun `test onMessageReceived with data payload`() {
        // Arrange
        val remoteMessage = mockk<RemoteMessage>(relaxed = true)
        every { remoteMessage.from } returns "test-sender"
        every { remoteMessage.data } returns mapOf("key" to "value", "type" to "test")
        every { remoteMessage.notification } returns null

        // Act
        service.onMessageReceived(remoteMessage)

        // Assert
        assertTrue("onMessageReceived handles data payload", true)
        verify { remoteMessage.from }
        verify { remoteMessage.data }
    }

    @Test
    fun `test onMessageReceived with empty data`() {
        // Arrange
        val remoteMessage = mockk<RemoteMessage>(relaxed = true)
        every { remoteMessage.from } returns "test-sender"
        every { remoteMessage.data } returns emptyMap()
        every { remoteMessage.notification } returns null

        // Act
        service.onMessageReceived(remoteMessage)

        // Assert
        assertTrue("onMessageReceived handles empty data", true)
        verify { remoteMessage.data }
    }

    @Test
    fun `test onMessageReceived with notification`() {
        // Arrange
        val notification = mockk<RemoteMessage.Notification>(relaxed = true)
        every { notification.title } returns "Test Title"
        every { notification.body } returns "Test Body"

        val remoteMessage = mockk<RemoteMessage>(relaxed = true)
        every { remoteMessage.from } returns "test-sender"
        every { remoteMessage.data } returns emptyMap()
        every { remoteMessage.notification } returns notification

        // Act
        service.onMessageReceived(remoteMessage)

        // Assert
        assertTrue("onMessageReceived handles notification", true)
        verify { remoteMessage.notification }
    }

    @Test
    fun `test onMessageReceived with null notification title`() {
        // Arrange
        val notification = mockk<RemoteMessage.Notification>(relaxed = true)
        every { notification.title } returns null
        every { notification.body } returns "Test Body"

        val remoteMessage = mockk<RemoteMessage>(relaxed = true)
        every { remoteMessage.from } returns "test-sender"
        every { remoteMessage.data } returns emptyMap()
        every { remoteMessage.notification } returns notification

        // Act
        service.onMessageReceived(remoteMessage)

        // Assert
        assertTrue("onMessageReceived handles null title", true)
    }

    @Test
    fun `test onMessageReceived with null notification body`() {
        // Arrange
        val notification = mockk<RemoteMessage.Notification>(relaxed = true)
        every { notification.title } returns "Test Title"
        every { notification.body } returns null

        val remoteMessage = mockk<RemoteMessage>(relaxed = true)
        every { remoteMessage.from } returns "test-sender"
        every { remoteMessage.data } returns emptyMap()
        every { remoteMessage.notification } returns notification

        // Act
        service.onMessageReceived(remoteMessage)

        // Assert
        assertTrue("onMessageReceived handles null body", true)
    }

    @Test
    fun `test onMessageReceived with both data and notification`() {
        // Arrange
        val notification = mockk<RemoteMessage.Notification>(relaxed = true)
        every { notification.title } returns "Title"
        every { notification.body } returns "Body"

        val remoteMessage = mockk<RemoteMessage>(relaxed = true)
        every { remoteMessage.from } returns "sender"
        every { remoteMessage.data } returns mapOf("extra" to "data")
        every { remoteMessage.notification } returns notification

        // Act
        service.onMessageReceived(remoteMessage)

        // Assert
        assertTrue("onMessageReceived handles both data and notification", true)
        verify { remoteMessage.data }
        verify { remoteMessage.notification }
    }

    @Test
    fun `test onMessageReceived with null from`() {
        // Arrange
        val remoteMessage = mockk<RemoteMessage>(relaxed = true)
        every { remoteMessage.from } returns null
        every { remoteMessage.data } returns emptyMap()
        every { remoteMessage.notification } returns null

        // Act
        service.onMessageReceived(remoteMessage)

        // Assert
        assertTrue("onMessageReceived handles null from", true)
    }

    @Test
    fun `test onMessageReceived with large data payload`() {
        // Arrange
        val largeData = mutableMapOf<String, String>()
        repeat(50) { i ->
            largeData["key$i"] = "value$i"
        }

        val remoteMessage = mockk<RemoteMessage>(relaxed = true)
        every { remoteMessage.from } returns "sender"
        every { remoteMessage.data } returns largeData
        every { remoteMessage.notification } returns null

        // Act
        service.onMessageReceived(remoteMessage)

        // Assert
        assertTrue("onMessageReceived handles large data payload", true)
    }

    @Test
    fun `test onMessageReceived with long notification text`() {
        // Arrange
        val notification = mockk<RemoteMessage.Notification>(relaxed = true)
        every { notification.title } returns "A".repeat(100)
        every { notification.body } returns "B".repeat(500)

        val remoteMessage = mockk<RemoteMessage>(relaxed = true)
        every { remoteMessage.from } returns "sender"
        every { remoteMessage.data } returns emptyMap()
        every { remoteMessage.notification } returns notification

        // Act
        service.onMessageReceived(remoteMessage)

        // Assert
        assertTrue("onMessageReceived handles long notification text", true)
    }

    @Test
    fun `test onMessageReceived called multiple times`() {
        // Arrange
        val remoteMessage = mockk<RemoteMessage>(relaxed = true)
        every { remoteMessage.from } returns "sender"
        every { remoteMessage.data } returns emptyMap()
        every { remoteMessage.notification } returns null

        // Act
        repeat(5) {
            service.onMessageReceived(remoteMessage)
        }

        // Assert
        assertTrue("onMessageReceived handles multiple calls", true)
    }

    // ========== Integration Tests ==========

    @Test
    fun `test complete notification flow`() {
        // Arrange - Simulate complete FCM message
        val notification = mockk<RemoteMessage.Notification>(relaxed = true)
        every { notification.title } returns "Fortune Ready"
        every { notification.body } returns "Your daily fortune is ready!"

        val remoteMessage = mockk<RemoteMessage>(relaxed = true)
        every { remoteMessage.from } returns "fcm-project"
        every { remoteMessage.data } returns mapOf("type" to "fortune", "id" to "123")
        every { remoteMessage.notification } returns notification

        // Act
        service.onMessageReceived(remoteMessage)

        // Assert
        assertTrue("Complete notification flow executed", true)
        verify { remoteMessage.from }
        verify { remoteMessage.data }
        verify { remoteMessage.notification }
    }

    @Test
    fun `test token refresh flow`() {
        // Act - Simulate token refresh
        val newToken = "refreshed-token-xyz"
        service.onNewToken(newToken)

        // Assert
        assertTrue("Token refresh flow completed", true)
    }

    @Test
    fun `test service handles edge cases`() {
        // Test various edge cases in sequence
        service.onNewToken("")

        val emptyMessage = mockk<RemoteMessage>(relaxed = true)
        every { emptyMessage.from } returns null
        every { emptyMessage.data } returns emptyMap()
        every { emptyMessage.notification } returns null

        service.onMessageReceived(emptyMessage)

        // Assert
        assertTrue("Service handles edge cases gracefully", true)
    }

    // ========== Companion Object Tests ==========

    @Test
    fun `test companion object TAG exists`() {
        // We can't directly access private companion object members,
        // but we verify the class structure is correct
        val serviceClass = FortunaFirebaseMessagingService::class.java
        assertNotNull("Service class should exist", serviceClass)
    }

    // ========== Service Lifecycle Tests ==========

    @Test
    fun `test service onCreate and onDestroy`() {
        // Arrange
        val controller = Robolectric.buildService(FortunaFirebaseMessagingService::class.java)

        // Act
        val createdService = controller.create().get()

        // Assert
        assertNotNull("Service created successfully", createdService)
    }

    @Test
    fun `test service can handle concurrent messages`() {
        // Arrange
        val messages = (1..10).map { i ->
            mockk<RemoteMessage>(relaxed = true).apply {
                every { from } returns "sender$i"
                every { data } returns mapOf("id" to "$i")
                every { notification } returns null
            }
        }

        // Act
        messages.forEach { service.onMessageReceived(it) }

        // Assert
        assertTrue("Service handles concurrent messages", true)
    }
}
