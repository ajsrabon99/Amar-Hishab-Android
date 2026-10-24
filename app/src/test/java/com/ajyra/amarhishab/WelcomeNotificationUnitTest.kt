package com.ajyra.amarhishab

import android.content.Context
import com.ajyra.amarhishab.data.local.NotificationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WelcomeNotificationUnitTest {

    private lateinit var context: Context
    private lateinit var repository: NotificationRepository

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("amar_hishab_notifications", Context.MODE_PRIVATE).edit().clear().commit()
        repository = NotificationRepository.getInstance(context)
    }

    @Test
    fun testFirstLaunchWelcomeNotificationFiresOnceAndDoesNotDuplicate() {
        // First launch trigger
        repository.triggerFirstLaunchWelcomeNotificationOnce()

        val notificationsAfterFirst = repository.notifications.value
        val welcomeNotif = notificationsAfterFirst.find { it.id == "welcome_midnight_serenity" }
        assertNotNull("Welcome notification must be present after first launch", welcomeNotif)
        assertEquals("Welcome to Midnight Serenity 🌙", welcomeNotif?.titleEn)
        assertEquals(
            "A calmer new look for your daily finances. Explore the latest Amar Hishab experience and keep your finances organized, one day at a time.",
            welcomeNotif?.messageEn
        )

        val countAfterFirst = notificationsAfterFirst.count { it.id == "welcome_midnight_serenity" }
        assertEquals(1, countAfterFirst)

        // Subsequent launch trigger (should NOT duplicate)
        repository.triggerFirstLaunchWelcomeNotificationOnce()
        val notificationsAfterSecond = repository.notifications.value
        val countAfterSecond = notificationsAfterSecond.count { it.id == "welcome_midnight_serenity" }
        assertEquals(1, countAfterSecond)
    }
}
