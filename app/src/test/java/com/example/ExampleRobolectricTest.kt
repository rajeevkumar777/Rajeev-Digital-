package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BillingType
import com.example.data.model.ServiceCatalogDefaults
import com.example.data.model.ServiceCategory
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.BrandSplashScreen
import com.example.ui.theme.RajeevDigitalTheme
import com.example.util.FormatUtils
import com.example.util.WhatsAppHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name and firestore database id resources resolve`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Rajeev Digital", context.getString(R.string.app_name))
        assertTrue(context.getString(R.string.firestore_database_id).isNotBlank())
    }

    @Test
    fun `service catalogue includes all 6 categories and confirmed pricing only`() {
        val services = ServiceCatalogDefaults.defaultServices
        assertEquals(6, services.map { it.category }.distinct().size)
        assertTrue(ServiceCategory.entries.all { cat -> services.any { it.category == cat } })

        val fixed = services.filter { it.fixedPrice }
        assertEquals(6, fixed.size)

        val insta = fixed.first { it.name == "Instagram Management" }
        assertEquals(14999.0, insta.price, 0.01)
        assertEquals(BillingType.MONTHLY, insta.billingType)

        val yt = fixed.first { it.name == "YouTube SEO & Growth" }
        assertEquals(9999.0, yt.price, 0.01)
        assertEquals(BillingType.MONTHLY, yt.billingType)

        val reels = fixed.first { it.name == "Reels/Shorts Content" }
        assertEquals(7999.0, reels.price, 0.01)
        assertEquals(BillingType.MONTHLY, reels.billingType)

        val shopify = fixed.first { it.name == "Shopify Store Setup" }
        assertEquals(24999.0, shopify.price, 0.01)
        assertEquals(BillingType.ONE_TIME, shopify.billingType)

        val auto = fixed.first { it.name == "Automation Setup" }
        assertEquals(17999.0, auto.price, 0.01)
        assertEquals(BillingType.ONE_TIME, auto.billingType)

        val consult = fixed.first { it.name == "1-on-1 Consulting" }
        assertEquals(4999.0, consult.price, 0.01)
        assertEquals(BillingType.PER_SESSION, consult.billingType)

        val unpriced = services.filterNot { it.fixedPrice }
        assertTrue(unpriced.isNotEmpty())
        assertTrue(unpriced.all { it.price == 0.0 && it.billingType == BillingType.CUSTOM_QUOTE })
    }

    @Test
    fun `format utils and whatsapp sanitization work accurately`() {
        assertEquals("₹14,999", FormatUtils.formatInr(14999.0))
        assertEquals("₹1,49,999", FormatUtils.formatInr(149999.0))
        assertEquals("₹12,34,567.50", FormatUtils.formatInr(1234567.5))
        assertEquals("918307229361", WhatsAppHelper.sanitizePhoneForWa("+91 8307229361"))
        assertEquals("919876543210", WhatsAppHelper.sanitizePhoneForWa("9876543210"))
        assertFalse(WhatsAppHelper.sanitizePhoneForWa("").isBlank())
    }

    @Test
    fun `auth screen renders Google Sign-In and WhatsApp desk buttons`() {
        composeTestRule.setContent {
            RajeevDigitalTheme {
                AuthScreen(onAuthSuccess = {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("auth_whatsapp_support_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `splash screen renders brand reveal`() {
        composeTestRule.setContent {
            RajeevDigitalTheme {
                BrandSplashScreen(onFinished = {})
            }
        }
        composeTestRule.waitForIdle()
    }
}
