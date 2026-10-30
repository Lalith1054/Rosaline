package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProductEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Rosaline", appName)
    }

    @Test
    fun `calculate elapsed days for active and stopped product`() {
        val start = 1_700_000_000_000L
        val fourteenDaysLater = start + TimeUnit.DAYS.toMillis(14) + TimeUnit.HOURS.toMillis(5)

        val activeProduct = ProductEntry(
            id = 1L,
            name = "Body Wash",
            startTimeMillis = start,
            endTimeMillis = null,
            isActive = true
        )
        assertTrue(activeProduct.isActive)
        assertEquals(14L, activeProduct.elapsedDaysCount(fourteenDaysLater))

        val stoppedProduct = activeProduct.copy(
            endTimeMillis = fourteenDaysLater,
            isActive = false
        )
        val thirtyDaysLater = start + TimeUnit.DAYS.toMillis(30)
        assertFalse(stoppedProduct.isActive)
        // Once stopped, elapsed days stays locked at 14 even when checked later
        assertEquals(14L, stoppedProduct.elapsedDaysCount(thirtyDaysLater))
    }
}
