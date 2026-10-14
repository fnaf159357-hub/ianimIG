package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmScheduler
import com.example.data.model.ReminderItem
import com.example.ui.components.CategoryData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("نغم المواعيد", appName)
    }

    @Test
    fun `category metadata returns valid titles and icons`() {
        val workMeta = CategoryData.get("work")
        assertEquals("عمل", workMeta.title)
        assertNotNull(workMeta.icon)

        val healthMeta = CategoryData.get("health")
        assertEquals("صحة ودواء", healthMeta.title)
    }

    @Test
    fun `calculate next trigger time for daily reminder produces future timestamp`() {
        val reminder = ReminderItem(
            id = 1,
            title = "طبيب",
            timeHour = 10,
            timeMinute = 30,
            isDaily = true
        )
        val triggerTime = AlarmScheduler.calculateNextTriggerTime(reminder)
        assertTrue(triggerTime >= System.currentTimeMillis() - 60000L)
    }
}
