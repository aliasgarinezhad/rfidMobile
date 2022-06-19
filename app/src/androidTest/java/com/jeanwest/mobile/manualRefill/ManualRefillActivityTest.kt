package com.jeanwest.mobile.manualRefill

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import coil.annotation.ExperimentalCoilApi
import org.junit.Rule
import org.junit.Test

@ExperimentalCoilApi
class ManualRefillActivityTest {

    @get:Rule
    val manualRefillActivity = createAndroidComposeRule<ManualRefillActivity>()

    //scan some epcs and barcodes, delete process
    @Test
    fun manualRefillActivityTest1() {

        clearUserData()

        manualRefillActivity.onNodeWithText("جستجو").performClick()
        manualRefillActivity.waitForIdle()
        manualRefillActivity.onNodeWithText("کد محصول").performTextClearance()
        manualRefillActivity.onNodeWithText("کد محصول").performTextInput("11531052")
        manualRefillActivity.waitForIdle()
        manualRefillActivity.onNodeWithText("11531052").performImeAction()
        manualRefillActivity.waitForIdle()

        Thread.sleep(2000)
        manualRefillActivity.waitForIdle()


        manualRefillActivity.onAllNodesWithTag("items")[0].performClick()
        manualRefillActivity.waitForIdle()
        manualRefillActivity.onAllNodesWithTag("items")[1].performClick()
        manualRefillActivity.waitForIdle()
        manualRefillActivity.onAllNodesWithTag("items")[2].performClick()
        manualRefillActivity.waitForIdle()
        manualRefillActivity.onAllNodesWithTag("items")[3].performClick()
        manualRefillActivity.waitForIdle()
        manualRefillActivity.onAllNodesWithTag("items")[3].performClick()
        manualRefillActivity.waitForIdle()

        manualRefillActivity.onNodeWithText("شارژ").performClick()
        manualRefillActivity.waitForIdle()

        manualRefillActivity.onAllNodesWithTag("items").assertCountEquals(4)
        manualRefillActivity.waitForIdle()
    }

    //test output apk file

    private fun clearUserData() {

        /*manualRefillActivity.activity.scannedProducts.forEach {
            manualRefillActivity.activity.selectedProductCodes.add(it.KBarCode)
        }
        manualRefillActivity.activity.clear()


        manualRefillActivity.activity.runOnUiThread {
            manualRefillActivity.activity.recreate()
        }
        manualRefillActivity.waitForIdle()
        Thread.sleep(1000)
        manualRefillActivity.waitForIdle()*/
    }
}

