package com.jeanwest.mobile.write


import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jeanwest.mobile.MainActivity
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.entity.UHFTAGInfo
import org.junit.Rule
import org.junit.Test


// before starting test, copy 1000000-1000120 serial range in iot hub
class WriteActivityTest {

    @get:Rule
    var writeActivity = createAndroidComposeRule<WriteActivity>()

    //write 100 stuffs
    @Test
    fun addProductTest1() {

        val header = 48
        val company = 101
        val partition = 0
        val filter = 0
        val serialNumberRange = 1000000L..1000050L

        val uhfTagInfo = UHFTAGInfo()
        uhfTagInfo.epc = "E28011702000015F195D0A17"
        uhfTagInfo.tid = "E28011702000015F195D0A18"

        MainActivity.token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjQwMTYsIm5hbWUiOiI0MDE2IiwiaWF0IjoxNjM5NTU3NDA0LCJleHAiOjE2OTc2MTgyMDR9.5baJVQbpJwTEJCm3nW4tE8hW8AWseN0qauIuBPFK5pQ"

        for (i in serialNumberRange) {

            RFIDWithUHFUART.uhfTagInfo.clear()
            RFIDWithUHFUART.writtenUhfTagInfo.tid = ""
            RFIDWithUHFUART.writtenUhfTagInfo.epc = ""

            repeat(3) {
                RFIDWithUHFUART.uhfTagInfo.add(uhfTagInfo)
            }

            writeActivity.waitForIdle()

            writeActivity.activity.onKeyDown(280, KeyEvent(ACTION_DOWN, 280))
            writeActivity.waitForIdle()

            writeActivity.activity.onKeyDown(280, KeyEvent(ACTION_DOWN, 280))

            writeActivity.waitForIdle()
            writeActivity.waitForIdle()
            Thread.sleep(500)

            assert(epcDecoder(RFIDWithUHFUART.writtenUhfTagInfo.epc).item == 130290L)
            assert(epcDecoder(RFIDWithUHFUART.writtenUhfTagInfo.epc).serial == i)
            assert(epcDecoder(RFIDWithUHFUART.writtenUhfTagInfo.epc).header == header)
            assert(epcDecoder(RFIDWithUHFUART.writtenUhfTagInfo.epc).company == company)
            assert(epcDecoder(RFIDWithUHFUART.writtenUhfTagInfo.epc).filter == filter)
            assert(epcDecoder(RFIDWithUHFUART.writtenUhfTagInfo.epc).partition == partition)
        }
    }

    //Write one unProgrammed stuff that placed between four programmed stuffs

    @Test
    fun addProductTest3() {

        RFIDWithUHFUART.uhfTagInfo.clear()
        RFIDWithUHFUART.writtenUhfTagInfo.tid = ""
        RFIDWithUHFUART.writtenUhfTagInfo.epc = ""

        MainActivity.token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjQwMTYsIm5hbWUiOiI0MDE2IiwiaWF0IjoxNjM5NTU3NDA0LCJleHAiOjE2OTc2MTgyMDR9.5baJVQbpJwTEJCm3nW4tE8hW8AWseN0qauIuBPFK5pQ"

        var uhfTagInfo = UHFTAGInfo()
        uhfTagInfo.epc = "308011702000015F195D0A17"
        uhfTagInfo.tid = "E28011702000015F195D0A18"
        RFIDWithUHFUART.uhfTagInfo.add(uhfTagInfo)

        uhfTagInfo = UHFTAGInfo()
        uhfTagInfo.epc = "308011702000015F195D0A17"
        uhfTagInfo.tid = "E28011702000015F195D0A19"
        RFIDWithUHFUART.uhfTagInfo.add(uhfTagInfo)

        uhfTagInfo = UHFTAGInfo()
        uhfTagInfo.epc = "E28011702000015F195D0A17"
        uhfTagInfo.tid = "E28011702000015F195D0A17"
        RFIDWithUHFUART.uhfTagInfo.add(uhfTagInfo)

        uhfTagInfo = UHFTAGInfo()
        uhfTagInfo.epc = "308011702000015F195D0A17"
        uhfTagInfo.tid = "E28011702000015F195D0A16"
        RFIDWithUHFUART.uhfTagInfo.add(uhfTagInfo)

        uhfTagInfo = UHFTAGInfo()
        uhfTagInfo.epc = "308011702000015F195D0A17"
        uhfTagInfo.tid = "E28011702000015F195D0A15"
        RFIDWithUHFUART.uhfTagInfo.add(uhfTagInfo)

        writeActivity.activity.onKeyDown(280, KeyEvent(ACTION_DOWN, 280))

        writeActivity.waitForIdle()

        writeActivity.activity.onKeyDown(280, KeyEvent(ACTION_DOWN, 280))

        writeActivity.waitForIdle()

        Thread.sleep(1000)
        writeActivity.waitForIdle()

        assert(RFIDWithUHFUART.writtenUhfTagInfo.tid == "E28011702000015F195D0A17")
    }

    //rewrite programmed tag

    @Test
    fun addProductTest4() {

        RFIDWithUHFUART.uhfTagInfo.clear()
        RFIDWithUHFUART.writtenUhfTagInfo.tid = ""
        RFIDWithUHFUART.writtenUhfTagInfo.epc = ""

        MainActivity.token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjQwMTYsIm5hbWUiOiI0MDE2IiwiaWF0IjoxNjM5NTU3NDA0LCJleHAiOjE2OTc2MTgyMDR9.5baJVQbpJwTEJCm3nW4tE8hW8AWseN0qauIuBPFK5pQ"

        val uhfTagInfo = UHFTAGInfo()
        uhfTagInfo.epc = "308011702000015F195D0A17"
        uhfTagInfo.tid = "E28011702000015F195D0A17"
        repeat(5) {
            RFIDWithUHFUART.uhfTagInfo.add(uhfTagInfo)
        }

        writeActivity.activity.onKeyDown(280, KeyEvent(ACTION_DOWN, 280))

        writeActivity.waitForIdle()

        writeActivity.activity.onKeyDown(280, KeyEvent(ACTION_DOWN, 280))

        writeActivity.waitForIdle()
        Thread.sleep(1000)
        writeActivity.waitForIdle()

        writeActivity.onNodeWithText("بله").performClick()

        writeActivity.waitForIdle()
        Thread.sleep(1000)
        writeActivity.waitForIdle()

        assert(epcDecoder(RFIDWithUHFUART.writtenUhfTagInfo.epc).item == 130290L)
    }

    @Test
    fun addProductTest5() {

        RFIDWithUHFUART.uhfTagInfo.clear()
        RFIDWithUHFUART.writtenUhfTagInfo.tid = ""
        RFIDWithUHFUART.writtenUhfTagInfo.epc = ""

        MainActivity.token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjQwMTYsIm5hbWUiOiI0MDE2IiwiaWF0IjoxNjM5NTU3NDA0LCJleHAiOjE2OTc2MTgyMDR9.5baJVQbpJwTEJCm3nW4tE8hW8AWseN0qauIuBPFK5pQ"

        writeActivity.waitForIdle()

        writeActivity.activity.onKeyDown(280, KeyEvent(ACTION_DOWN, 280))

        writeActivity.waitForIdle()

        writeActivity.activity.onKeyDown(280, KeyEvent(ACTION_DOWN, 280))

        writeActivity.waitForIdle()

        Thread.sleep(2000)
        writeActivity.waitForIdle()

        assert(RFIDWithUHFUART.writtenUhfTagInfo.epc == "")
    }

    //Write one stuff and check whether result is correct by QR Code
    @Test
    fun addProductTest6() {

        RFIDWithUHFUART.uhfTagInfo.clear()
        RFIDWithUHFUART.writtenUhfTagInfo.tid = ""
        RFIDWithUHFUART.writtenUhfTagInfo.epc = ""

        MainActivity.token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOjQwMTYsIm5hbWUiOiI0MDE2IiwiaWF0IjoxNjM5NTU3NDA0LCJleHAiOjE2OTc2MTgyMDR9.5baJVQbpJwTEJCm3nW4tE8hW8AWseN0qauIuBPFK5pQ"

        writeActivity.onNodeWithTag("WriteActivityTagTypeDropDownList").performClick()
        writeActivity.waitForIdle()
        writeActivity.onNodeWithText("تگ کیوآر کد دار").performClick()
        writeActivity.waitForIdle()

        val uhfTagInfo = UHFTAGInfo()
        uhfTagInfo.epc = "E288011702000015F195D0A17"
        uhfTagInfo.tid = "E28" + "J64822109801099001"

        RFIDWithUHFUART.uhfTagInfo.add(uhfTagInfo)

        writeActivity.activity.onKeyDown(280, KeyEvent(ACTION_DOWN, 280))

        writeActivity.waitForIdle()

        writeActivity.activity.onKeyDown(280, KeyEvent(ACTION_DOWN, 280))

        writeActivity.waitForIdle()
        Thread.sleep(1000)
        writeActivity.waitForIdle()

        assert(epcDecoder(RFIDWithUHFUART.writtenUhfTagInfo.epc).item == 130290L)
    }

    private fun epcDecoder(epc: String): EPC {

        val binaryEPC =
            String.format("%64s", epc.substring(0, 16).toULong(16).toString(2))
                .replace(" ".toRegex(), "0") +
                    String.format("%32s", epc.substring(16, 24).toULong(16).toString(2))
                        .replace(" ".toRegex(), "0")
        val result = EPC(0, 0, 0, 0, 0L, 0L)
        result.header = binaryEPC.substring(0, 8).toInt(2)
        result.partition = binaryEPC.substring(8, 11).toInt(2)
        result.filter = binaryEPC.substring(11, 14).toInt(2)
        result.company = binaryEPC.substring(14, 26).toInt(2)
        result.item = binaryEPC.substring(26, 58).toLong(2)
        result.serial = binaryEPC.substring(58, 96).toLong(2)
        return result
    }

    data class EPC(
        var header: Int,
        var filter: Int,
        var partition: Int,
        var company: Int,
        var item: Long,
        var serial: Long
    )
}