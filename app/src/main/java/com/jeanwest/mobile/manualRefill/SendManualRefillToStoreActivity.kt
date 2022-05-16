package com.jeanwest.mobile.manualRefill

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NoConnectionError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeanwest.mobile.JalaliDate.JalaliDate
import com.jeanwest.mobile.MainActivity
import com.jeanwest.mobile.R
import com.jeanwest.mobile.iotHub.IotHub
import com.jeanwest.mobile.theme.ErrorSnackBar
import com.jeanwest.mobile.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalFoundationApi::class)
class SendManualRefillToStoreActivity : ComponentActivity() {

    private var uiList by mutableStateOf(mutableListOf<ManualRefillProduct>())
    private var fileName by mutableStateOf("ارسالی شارژ تاریخ ")
    private var openFileDialog by mutableStateOf(false)
    private var numberOfScanned by mutableStateOf(0)
    private var state = SnackbarHostState()
    private val apiTimeout = 30000
    private var isSubmitting by mutableStateOf(false)
    private lateinit var iotHubService: IotHub
    private var iotHubConnected = false
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as IotHub.LocalBinder
            iotHubService = binder.service
            iotHubConnected = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            iotHubConnected = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Page()
        }

        val type = object : TypeToken<List<ManualRefillProduct>>() {}.type

        uiList = Gson().fromJson(
            intent.getStringExtra("ManualRefillProducts"), type
        ) ?: mutableListOf()

        numberOfScanned = intent.getIntExtra("ManualRefillValidScannedProductsNumber", 0)

        val util = JalaliDate()
        fileName += util.currentShamsidate

        val intent = Intent(this, IotHub::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun exportFile() {

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("ارسالی به فروشگاه")

        val headerRow = sheet.createRow(sheet.physicalNumberOfRows)
        headerRow.createCell(0).setCellValue("کد جست و جو")
        headerRow.createCell(1).setCellValue("تعداد")

        uiList.forEach {
            val row = sheet.createRow(sheet.physicalNumberOfRows)
            row.createCell(0).setCellValue(it.KBarCode)
            row.createCell(1).setCellValue((it.scannedEPCNumber + it.scannedBarcodeNumber).toDouble())
        }

        val dir = File(this.getExternalFilesDir(null), "/")

        val outFile = File(dir, "$fileName.xlsx")

        val outputStream = FileOutputStream(outFile.absolutePath)
        workbook.write(outputStream)
        outputStream.flush()
        outputStream.close()

        val uri = FileProvider.getUriForFile(
            this,
            this.applicationContext.packageName + ".provider",
            outFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "application/octet-stream"
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        applicationContext.startActivity(shareIntent)
    }

    private fun sendToStore() {

        uiList.forEach {
            if (it.scannedBarcodeNumber + it.scannedEPCNumber > it.wareHouseNumber) {
                CoroutineScope(Dispatchers.Default).launch {
                    state.showSnackbar(
                        "تعداد ارسالی کالای ${it.KBarCode}" + " از موجودی انبار بیشتر است.",
                        null,
                        SnackbarDuration.Long
                    )
                }
                return
            }
        }

        isSubmitting = true

        val url = "https://rfid-api.avakatan.ir/stock-draft/refill"
        val request = object : JsonObjectRequest(Method.POST, url, null, {

            CoroutineScope(Dispatchers.Default).launch {
                state.showSnackbar(
                    "اجناس با موفقیت به فروشگاه حواله شدند",
                    null,
                    SnackbarDuration.Long
                )
            }

            sendLog(uiList)
            isSubmitting = false
            ManualRefillActivity.scannedBarcodeTable.clear()
            ManualRefillActivity.scannedEpcTable.clear()
            uiList.forEach{
                ManualRefillActivity.manualRefillProducts.removeAll { it1 ->
                    it1.KBarCode == it.KBarCode
                }
            }
            finish()

        }, {
            if (it is NoConnectionError) {
                CoroutineScope(Dispatchers.Default).launch {
                    state.showSnackbar(
                        "اینترنت قطع است. شبکه وای فای را بررسی کنید.",
                        null,
                        SnackbarDuration.Long
                    )
                }
            } else {
                CoroutineScope(Dispatchers.Default).launch {
                    state.showSnackbar(
                        it.toString(),
                        null,
                        SnackbarDuration.Long
                    )
                }
            }
            isSubmitting = false
        }) {
            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/json;charset=UTF-8"
                params["Authorization"] =
                    "Bearer " + MainActivity.token
                return params
            }

            override fun getBody(): ByteArray {

                val body = JSONObject()
                val products = JSONArray()

                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH)
                Log.e("time", sdf.format(Date()))

                uiList.forEach {
                    repeat(it.scannedEPCNumber + it.scannedBarcodeNumber) { _ ->
                        val productJson = JSONObject()
                        productJson.put("BarcodeMain_ID", it.primaryKey)
                        productJson.put("kbarcode", it.KBarCode)
                        productJson.put("K_Name", it.kName)
                        products.put(productJson)
                    }
                }

                body.put("desc", "برای تست")
                body.put("createDate", sdf.format(Date()))
                body.put("products", products)

                return body.toString().toByteArray()
            }
        }

        request.retryPolicy = DefaultRetryPolicy(
            apiTimeout,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        val queue = Volley.newRequestQueue(this)
        queue.add(request)
    }

    fun sendLog(manualRefillProducts: List<ManualRefillProduct>) {

        if(!iotHubConnected) {
            return
        }
        val workbook = XSSFWorkbook()
        val sheet: Sheet = workbook.createSheet("شارژ")
        val headerRow = sheet.createRow(sheet.physicalNumberOfRows)
        headerRow.createCell(0).setCellValue("بارکد")
        headerRow.createCell(1).setCellValue("تعداد")
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("yyyy.MM.dd'T'HH:mm:ssZ", Locale.ENGLISH)
        val dir = File(getExternalFilesDir(null), "/")
        val outFile = File(dir, "manualRefillLog" + sdf.format(Date()) + ".xlsx")
        for ((_, KBarCode, _, _, scannedNumber) in manualRefillProducts) {
            val row = sheet.createRow(sheet.physicalNumberOfRows)
            row.createCell(0).setCellValue(KBarCode)
            row.createCell(1).setCellValue(scannedNumber.toDouble())
            val outputStream = FileOutputStream(outFile.absolutePath)
            workbook.write(outputStream)
            outputStream.flush()
            outputStream.close()
        }
        iotHubService.sendFile(outFile)
    }

    @ExperimentalCoilApi
    @ExperimentalFoundationApi
    @Composable
    fun Page() {
        MyApplicationTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Scaffold(
                    topBar = { AppBar() },
                    content = { Content() },
                    bottomBar = { BottomAppBar() },
                    snackbarHost = { ErrorSnackBar(state) },
                )
            }
        }
    }

    @Composable
    fun BottomAppBar() {
        BottomAppBar(backgroundColor = MaterialTheme.colors.background) {

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "مجموع ارسالی: $numberOfScanned",
                    textAlign = TextAlign.Right,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                )

                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                ) {
                    Button(
                        onClick = {
                            if(!isSubmitting) {
                                sendToStore()
                            }
                        },
                    ) {
                        if(!isSubmitting) {
                            Text(text = "ارسال به فروشگاه")
                        } else {
                            Text(text = "در حال ارسال ...")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AppBar() {

        TopAppBar(

            navigationIcon = {
                IconButton(onClick = { finish() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = ""
                    )
                }
            },

            actions = {
                IconButton(onClick = { openFileDialog = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_share_24),
                        contentDescription = ""
                    )
                }
            },

            title = {
                Text(
                    text = stringResource(id = R.string.manualRefill),
                    modifier = Modifier
                        .padding(end = 15.dp)
                        .fillMaxSize()
                        .wrapContentSize(),
                    textAlign = TextAlign.Center,
                )
            }
        )
    }

    @ExperimentalCoilApi
    @ExperimentalFoundationApi
    @Composable
    fun Content() {

        Column {

            if (openFileDialog) {
                FileAlertDialog()
            }

            LazyColumn(modifier = Modifier.padding(top = 8.dp, bottom = 56.dp)) {

                items(uiList.size) { i ->
                    LazyColumnItem(i)
                }
            }
        }
    }

    @Composable
    fun LazyColumnItem(i: Int) {

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                .background(
                    color = MaterialTheme.colors.onPrimary,
                    shape = MaterialTheme.shapes.small,
                )
                .fillMaxWidth()
                .height(80.dp),
        ) {

            Image(
                painter = rememberImagePainter(
                    uiList[i].imageUrl,
                ),
                contentDescription = "",
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .padding(vertical = 8.dp, horizontal = 8.dp)
            )

            Row(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .fillMaxHeight()
            ) {

                Column(
                    modifier = Modifier
                        .weight(1.5F)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = uiList[i].name,
                        style = MaterialTheme.typography.h1,
                        textAlign = TextAlign.Right,
                    )

                    Text(
                        text = uiList[i].KBarCode,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Right,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1F)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {

                    Text(
                        text = "موجودی انبار: " + uiList[i].wareHouseNumber,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Right,
                    )

                    Text(
                        text = "اسکن شده: " + (uiList[i].scannedEPCNumber + uiList[i].scannedBarcodeNumber).toString(),
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Right,
                    )
                }
            }
        }
    }

    @Composable
    fun FileAlertDialog() {

        AlertDialog(

            buttons = {

                Column {

                    Text(
                        text = "نام فایل خروجی را وارد کنید", modifier = Modifier
                            .padding(top = 10.dp, start = 10.dp, end = 10.dp)
                    )

                    OutlinedTextField(
                        value = fileName, onValueChange = {
                            fileName = it
                        },
                        modifier = Modifier
                            .padding(top = 10.dp, start = 10.dp, end = 10.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    Button(modifier = Modifier
                        .padding(bottom = 10.dp, top = 10.dp, start = 10.dp, end = 10.dp)
                        .align(Alignment.CenterHorizontally),
                        onClick = {
                            openFileDialog = false
                            exportFile()
                        }) {
                        Text(text = "ذخیره")
                    }
                }
            },

            onDismissRequest = {
                openFileDialog = false
            }
        )
    }
}