package com.jeanwest.mobile.manualRefill


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.android.volley.NoConnectionError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeanwest.mobile.R
import com.jeanwest.mobile.logIn.UserLoginActivity
import com.jeanwest.mobile.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ManualRefillActivity : ComponentActivity() {

    private var fullName = ""
    private var username = ""
    private var token = ""

    //charge ui parameters
    private var isDataLoading by mutableStateOf(false)
    private var state = SnackbarHostState()
    var uiList = mutableStateListOf<ManualRefillProduct>()
    private var sumOfManualRefill by mutableStateOf(0)
    private var isSubmitting by mutableStateOf(false)
    private var isSubmitSelected by mutableStateOf(false)
    private var manualRefillProducts = mutableListOf<ManualRefillProduct>()

    // search ui parameters
    private var productCode by mutableStateOf("")
    private var searchUiList = mutableStateListOf<ManualRefillProduct>()
    private var filteredUiList = mutableStateListOf<ManualRefillProduct>()
    private var colorFilterValues = mutableStateListOf("همه رنگ ها")
    private var sizeFilterValues = mutableStateListOf("همه سایز ها")
    private var colorFilterValue by mutableStateOf("همه رنگ ها")
    private var sizeFilterValue by mutableStateOf("همه سایز ها")
    private var storeFilterValue = 0
    private val beep: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private var isCameraOn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Page()
        }
        loadMemory()

        if (username == "") {
            val intent =
                Intent(this, UserLoginActivity::class.java)
            intent.flags += Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        checkPermission()
        clearCash()
    }

    private fun clearCash() {
        deleteRecursive(cacheDir)
        deleteRecursive(codeCacheDir)
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.let {
                for (child in it) {
                    deleteRecursive(child)
                }
            }
        }
        fileOrDirectory.delete()
    }

    private fun checkPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                0
            )
        }
    }

    private fun sendManualRefillRequest() {

        if (sumOfManualRefill == 0) {
            CoroutineScope(Dispatchers.Default).launch {
                state.showSnackbar(
                    "هنوز کالایی برای شارژ انتخاب نکرده اید",
                    null,
                    SnackbarDuration.Long
                )
            }
            return
        }

        uiList.forEach {
            if (it.requestedNum > it.wareHouseNumber) {
                CoroutineScope(Dispatchers.Default).launch {
                    state.showSnackbar(
                        "تعداد درخواستی کالای ${it.KBarCode}" + " از موجودی انبار بیشتر است.",
                        null,
                        SnackbarDuration.Long
                    )
                }
                return
            }
        }

        isSubmitting = true

        val url = "https://rfid-api.avakatan.ir/charge-requests"
        val request = object : JsonArrayRequest(Method.POST, url, null, {

            CoroutineScope(Dispatchers.Default).launch {
                state.showSnackbar(
                    "درخواست برای انبار دار ارسال شد.",
                    null,
                    SnackbarDuration.Long
                )
            }
            isSubmitting = false
            uiList.clear()
            manualRefillProducts.clear()
            sumOfManualRefill = 0
            saveToMemory()

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
                Log.e("error", it.toString())
            }

            isSubmitting = false
        }) {
            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/json;charset=UTF-8"
                params["Authorization"] =
                    "Bearer $token"
                return params
            }

            override fun getBody(): ByteArray {

                val products = JSONArray()

                uiList.forEach {
                    repeat(it.requestedNum) { _ ->
                        val productJson = JSONObject()
                        productJson.put("BarcodeMainID", it.primaryKey)
                        productJson.put("KBarCode", it.KBarCode)
                        products.put(productJson)
                    }
                }

                return products.toString().toByteArray()
            }
        }

        val queue = Volley.newRequestQueue(this)
        queue.add(request)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {

        if (keyCode == 4) {
            back()
        }
        return true
    }

    private fun saveToMemory() {

        val memory = PreferenceManager.getDefaultSharedPreferences(this)
        val edit = memory.edit()

        edit.putString(
            "ManualRefillProducts",
            Gson().toJson(manualRefillProducts).toString()
        )

        edit.apply()
    }

    private fun loadMemory() {

        val memory = PreferenceManager.getDefaultSharedPreferences(this)

        val type = object : TypeToken<List<ManualRefillProduct>>() {}.type

        username = memory.getString("username", "") ?: ""
        token = memory.getString("accessToken", "") ?: ""
        fullName = memory.getString("userFullName", "") ?: ""

        storeFilterValue = memory.getInt("userLocationCode", 0)

        manualRefillProducts = Gson().fromJson(
            memory.getString("ManualRefillProducts", ""),
            type
        ) ?: ArrayList()

        uiList.addAll(manualRefillProducts)
        sumOfManualRefill = 0
        uiList.forEach {
            sumOfManualRefill += it.requestedNum
        }
    }


    private fun addToRefillList(KBarCode: String) {

        val productIndexInUiList = searchUiList.indexOfLast {
            it.KBarCode == KBarCode
        }

        manualRefillProducts.forEach {
            if (searchUiList[productIndexInUiList].KBarCode == it.KBarCode) {
                if (it.requestedNum >= it.wareHouseNumber) {
                    CoroutineScope(Dispatchers.Main).launch {
                        state.showSnackbar(
                            "تعداد درخواستی از موجودی انبار بیشتر است.",
                            null,
                            SnackbarDuration.Long,
                        )
                    }
                    return
                } else {
                    it.requestedNum++
                    filterUiList()
                    saveToMemory()
                    return
                }
            }
        }

        if (searchUiList[productIndexInUiList].wareHouseNumber == 0) {
            CoroutineScope(Dispatchers.Main).launch {
                state.showSnackbar(
                    "تعداد درخواستی از موجودی انبار بیشتر است.",
                    null,
                    SnackbarDuration.Long,
                )
            }
            return
        }

        manualRefillProducts.add(searchUiList[productIndexInUiList])
        manualRefillProducts[manualRefillProducts.indexOfLast {
            it.KBarCode == KBarCode
        }].requestedNum++

        filterUiList()
        saveToMemory()
    }

    private fun filterUiList() {

        searchUiList.forEach {
            var isRequested = false
            manualRefillProducts.forEach { it1 ->
                if (it.KBarCode == it1.KBarCode) {
                    it.requestedNum = it1.requestedNum
                    isRequested = true
                }
            }
            if (!isRequested) {
                it.requestedNum = 0
            }
        }

        /*val wareHouseFilterOutput = searchUiList.filter {
            it.wareHouseNumber > 0
        }*/

        val sizeFilterOutput = if (sizeFilterValue == "همه سایز ها") {
            searchUiList
        } else {
            searchUiList.filter {
                it.size == sizeFilterValue
            }
        }

        val colorFilterOutput = if (colorFilterValue == "همه رنگ ها") {
            sizeFilterOutput
        } else {
            sizeFilterOutput.filter {
                it.color == colorFilterValue
            }
        }
        filteredUiList.clear()
        filteredUiList.addAll(colorFilterOutput)
        sumOfManualRefill = 0
        uiList.clear()
        uiList.addAll(manualRefillProducts)
        uiList.forEach {
            sumOfManualRefill += it.requestedNum
        }
    }

    private fun getSimilarProducts() {

        if (productCode == "کد محصول") {
            CoroutineScope(Dispatchers.Default).launch {
                state.showSnackbar(
                    "لطفا کد محصول را وارد کنید.",
                    null,
                    SnackbarDuration.Long
                )
            }
        }

        isDataLoading = true

        searchUiList.clear()
        filteredUiList.clear()
        colorFilterValues = mutableStateListOf("همه رنگ ها")
        sizeFilterValues = mutableStateListOf("همه سایز ها")

        val url1 =
            "https://rfid-api.avakatan.ir/products/similars?DepartmentInfo_ID=$storeFilterValue&K_Bar_Code=$productCode"

        val request1 = JsonObjectRequest(url1, { response1 ->

            val products = response1.getJSONArray("products")

            if (products.length() > 0) {
                jsonArrayProcess(products)
                isDataLoading = false
            } else {

                val url2 =
                    "https://rfid-api.avakatan.ir/products/similars?DepartmentInfo_ID=$storeFilterValue&kbarcode=$productCode"

                val request2 = JsonObjectRequest(url2, { response2 ->

                    val products2 = response2.getJSONArray("products")

                    if (products2.length() > 0) {
                        jsonArrayProcess(products2)
                    }
                    isDataLoading = false
                }, { it2 ->
                    when (it2) {
                        is NoConnectionError -> {
                            CoroutineScope(Dispatchers.Default).launch {
                                state.showSnackbar(
                                    "اینترنت قطع است. شبکه وای فای را بررسی کنید.",
                                    null,
                                    SnackbarDuration.Long
                                )
                            }
                        }
                        else -> {
                            CoroutineScope(Dispatchers.Default).launch {
                                state.showSnackbar(
                                    it2.toString(),
                                    null,
                                    SnackbarDuration.Long
                                )
                            }
                        }
                    }
                    isDataLoading = false
                })
                val queue2 = Volley.newRequestQueue(this)
                queue2.add(request2)
            }
        }, {
            when (it) {
                is NoConnectionError -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        state.showSnackbar(
                            "اینترنت قطع است. شبکه وای فای را بررسی کنید.",
                            null,
                            SnackbarDuration.Long
                        )
                    }
                }
                else -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        state.showSnackbar(
                            it.toString(),
                            null,
                            SnackbarDuration.Long
                        )
                    }
                }
            }
            isDataLoading = false
        })

        val queue = Volley.newRequestQueue(this)
        queue.add(request1)
    }

    private fun jsonArrayProcess(similarProductsJsonArray: JSONArray) {

        for (i in 0 until similarProductsJsonArray.length()) {

            val json = similarProductsJsonArray.getJSONObject(i)

            colorFilterValues.add(json.getString("Color"))
            sizeFilterValues.add(json.getString("Size"))

            val product = ManualRefillProduct(
                name = json.getString("productName"),
                KBarCode = json.getString("KBarCode"),
                imageUrl = json.getString("ImgUrl"),
                wareHouseNumber = json.getInt("dbCountDepo"),
                productCode = json.getString("K_Bar_Code"),
                size = json.getString("Size"),
                color = json.getString("Color"),
                originalPrice = json.getString("OrigPrice"),
                salePrice = json.getString("SalePrice"),
                primaryKey = json.getLong("BarcodeMain_ID"),
                rfidKey = json.getLong("RFID"),
                kName = json.getString("K_Name"),
                requestedNum = 0,
                storeNumber = json.getInt("dbCountStore"),
            )
            searchUiList.add(product)
        }

        colorFilterValues = colorFilterValues.distinct().toMutableStateList()
        sizeFilterValues = sizeFilterValues.distinct().toMutableStateList()

        getScannedProductProperties()
    }

    private fun getScannedProductProperties() {

        isDataLoading = true

        val url = "https://rfid-api.avakatan.ir/products/v4"

        val request = object : JsonObjectRequest(Method.POST, url, null, {

            val barcodes = it.getJSONArray("KBarCodes")
            if (barcodes.length() != 0) {
                colorFilterValue = barcodes.getJSONObject(0).getString("Color")
            }
            productCode = searchUiList[0].productCode
            filterUiList()
            isDataLoading = false

        }, {
            when (it) {
                is NoConnectionError -> {

                    CoroutineScope(Dispatchers.Default).launch {
                        state.showSnackbar(
                            "اینترنت قطع است. شبکه وای فای را بررسی کنید.",
                            null,
                            SnackbarDuration.Long
                        )
                    }
                }
                else -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        state.showSnackbar(
                            it.toString(),
                            null,
                            SnackbarDuration.Long
                        )
                    }
                }
            }
            isDataLoading = false

        }) {
            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/json;charset=UTF-8"
                params["Authorization"] = "Bearer $token"
                return params
            }

            override fun getBody(): ByteArray {
                val json = JSONObject()
                val epcArray = JSONArray()
                val barcodeArray = JSONArray()

                barcodeArray.put(productCode)

                json.put("epcs", epcArray)
                json.put("KBarCodes", barcodeArray)
                return json.toString().toByteArray()
            }
        }

        val queue = Volley.newRequestQueue(this)
        queue.add(request)
    }

    private fun back() {

        if (isCameraOn) {
            isCameraOn = false
        } else {
            saveToMemory()
            finish()
        }
    }

    @Composable
    fun Page() {
        MyApplicationTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Scaffold(
                    bottomBar = { BottomBar() },
                    content = {
                        if (isSubmitSelected) {
                            Content()
                        } else {
                            SearchContent()
                        }
                    },
                    snackbarHost = { ErrorSnackBar(state) },
                    floatingActionButton = {
                        if (!isSubmitSelected) {
                            BarcodeScanButton()
                        }
                    },
                    floatingActionButtonPosition = if (!isSubmitSelected) {
                        FabPosition.Center
                    } else {
                        FabPosition.End
                    },
                )
            }
        }
    }

    @Composable
    fun SendRequestButton() {

        Box(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .padding(bottom = 56.dp)
                    .shadow(6.dp, RoundedCornerShape(0.dp))
                    .background(Color.White, RoundedCornerShape(0.dp))
                    .height(100.dp)
                    .align(BottomCenter),
            ) {

                Button(modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .align(Center)
                    .fillMaxWidth()
                    .align(Center),
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Jeanswest,
                        disabledBackgroundColor = DisableButtonColor,
                        disabledContentColor = Color.White
                    ),
                    onClick = {
                        if (!isSubmitting) {
                            sendManualRefillRequest()
                        }
                    }) {
                    Text(
                        text = if (isSubmitting) "در حال ارسال درخواست" else "ارسال درخواست به انباردار",
                        style = Typography.body1,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun BarcodeScanButton() {
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(modifier = Modifier
                .align(BottomStart)
                .padding(start = 16.dp), onClick = {
                isCameraOn = true
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_barcode_scan),
                    contentDescription = "",
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 8.dp)
                )
                Text(
                    text = "اسکن کالای جدید",
                    style = Typography.h1
                )
            }
        }
    }

    @Composable
    fun BottomBar() {

        BottomNavigation(backgroundColor = BottomBar) {
            BottomNavigationItem(
                selected = !isSubmitSelected,
                onClick = {
                    isSubmitSelected = false
                    filterUiList()
                },
                selectedContentColor = Jeanswest,
                unselectedContentColor = unselectedColor,
                icon = {

                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = "",
                            modifier = Modifier
                                .padding(top = 0.dp)
                                .size(24.dp)
                                .align(Center)
                        )
                    }
                },
                label = {
                    Text(text = "شارژ")
                }
            )

            BottomNavigationItem(
                selected = isSubmitSelected,
                onClick = {
                    isSubmitSelected = true
                    uiList.clear()
                    uiList.addAll(manualRefillProducts)
                    sumOfManualRefill = 0
                    uiList.forEach {
                        sumOfManualRefill += it.requestedNum
                    }
                },
                selectedContentColor = Jeanswest,
                unselectedContentColor = unselectedColor,
                icon = {
                    Box(modifier = Modifier.height(30.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.submit),
                            contentDescription = "",
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(24.dp)
                                .align(Center)
                        )
                        if (sumOfManualRefill > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 0.dp, end = 24.dp)
                                    .background(
                                        shape = RoundedCornerShape(24.dp),
                                        color = warningColor
                                    )
                                    .size(18.dp)
                                    .align(TopCenter)
                            ) {
                                Text(
                                    text = sumOfManualRefill.toString(),
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .align(Center),
                                    style = Typography.caption,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                },
                label = {
                    Text(text = "ثبت درخواست")
                }
            )
        }
    }

    @Composable
    fun Content() {

        Column {

            Column(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 0.dp)
                    .background(
                        MaterialTheme.colors.onPrimary,
                        shape = MaterialTheme.shapes.small
                    )
                    .fillMaxWidth()
            ) {

                Text(
                    "ثبت درخواست " + "(" + sumOfManualRefill + ")",
                    style = Typography.h1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Right,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Background, shape = Shapes.small)
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                )
            }

            if (uiList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 56.dp)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .align(Center)
                            .width(256.dp)
                    ) {
                        Box(


                            modifier = Modifier
                                .background(color = Color.White, shape = Shapes.medium)
                                .size(256.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_empty_box),
                                contentDescription = "",
                                tint = Color.Unspecified,
                                modifier = Modifier.align(Center)
                            )
                        }

                        Text(
                            "هنوز کالایی برای ثبت درخواست شارژ انتخاب نکرده اید",
                            style = Typography.h1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp, start = 4.dp, end = 4.dp),
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.padding(top = 2.dp, bottom = 156.dp)) {

                    items(uiList.size) { i ->
                        LazyColumnItem(i)
                    }
                }
            }
        }

        if (!uiList.isEmpty()) {
            SendRequestButton()
        }
    }

    @Composable
    fun LazyColumnItem(i: Int) {

        val topPaddingClearButton = if (i == 0) 8.dp else 4.dp

        Box {

            Item(i, uiList)

            Box(
                modifier = Modifier
                    .padding(top = topPaddingClearButton, end = 8.dp)
                    .background(
                        shape = RoundedCornerShape(36.dp),
                        color = deleteCircleColor
                    )
                    .size(30.dp)
                    .align(Alignment.TopEnd)
                    .clickable {

                        manualRefillProducts.remove(uiList[i])
                        saveToMemory()
                        uiList.clear()
                        uiList.addAll(manualRefillProducts)
                        sumOfManualRefill = 0
                        uiList.forEach {
                            sumOfManualRefill += it.requestedNum
                        }
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_clear_24),
                    contentDescription = "",
                    tint = deleteColor,
                    modifier = Modifier
                        .align(Center)
                        .size(20.dp)
                )
            }
        }
    }

    @Composable
    fun SearchContent() {

        Column(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .padding(bottom = 0.dp)
                    .shadow(elevation = 6.dp, shape = MaterialTheme.shapes.large)
                    .background(
                        color = MaterialTheme.colors.onPrimary,
                        shape = MaterialTheme.shapes.large
                    )
                    .fillMaxWidth(),
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProductCodeTextField(
                        modifier = Modifier
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 14.dp)
                            .weight(1F)
                            .fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {

                    FilterDropDownList(
                        modifier = Modifier
                            .padding(start = 16.dp, bottom = 16.dp),
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_color_lens_24),
                                contentDescription = "",
                                tint = iconColor,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 4.dp)
                            )
                        },
                        text = {
                            Text(
                                style = MaterialTheme.typography.body2,
                                text = colorFilterValue,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 4.dp)
                            )
                        },
                        onClick = {
                            colorFilterValue = it
                            filterUiList()
                        },
                        values = colorFilterValues
                    )

                    FilterDropDownList(
                        modifier = Modifier
                            .padding(start = 16.dp, bottom = 16.dp),
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.size),
                                contentDescription = "",
                                tint = iconColor,
                                modifier = Modifier
                                    .size(28.dp)
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 6.dp)
                            )
                        },
                        text = {
                            Text(
                                text = sizeFilterValue,
                                style = MaterialTheme.typography.body2,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 6.dp)
                            )
                        },
                        onClick = {
                            sizeFilterValue = it
                            filterUiList()
                        },
                        values = sizeFilterValues
                    )
                }
            }

            if (isDataLoading) {
                Row(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(), horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colors.primary)

                    if (isDataLoading) {
                        Text(
                            text = "در حال بارگذاری",
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            BarcodeScannerWithCamera(isCameraOn, this@ManualRefillActivity) { barcodes ->
                isCameraOn = false
                productCode = barcodes[0].displayValue.toString()
                beep.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                getSimilarProducts()
            }

            if (filteredUiList.isEmpty()) {
                EmptyList()
            } else {
                LazyColumn(modifier = Modifier.padding(top = 0.dp, bottom = 56.dp)) {

                    items(filteredUiList.size) { i ->
                        Item(i, filteredUiList, true) {
                            addToRefillList(filteredUiList[i].KBarCode)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ProductCodeTextField(
        modifier: Modifier
    ) {

        val focusManager = LocalFocusManager.current

        OutlinedTextField(
            textStyle = MaterialTheme.typography.body2,

            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = ""
                )
            },
            value = productCode,
            onValueChange = {
                productCode = it
            },
            modifier = modifier
                .testTag("SearchProductCodeTextField")
                .background(
                    color = MaterialTheme.colors.secondary,
                    shape = MaterialTheme.shapes.small
                ),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                isCameraOn = false
                getSimilarProducts()
            }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                unfocusedBorderColor = MaterialTheme.colors.secondary
            ),
            placeholder = { Text(text = "کد محصول") }
        )
    }

    @Composable
    fun EmptyList() {
        Box(
            modifier = Modifier
                .padding(bottom = 56.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .align(Center)
                    .width(256.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(color = Color.White, shape = Shapes.medium)
                        .size(256.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_big_barcode_scan),
                        contentDescription = "",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .align(Center)
                            .clickable { isCameraOn = true }
                    )
                }

                Text(
                    "بارکد را اسکن یا کد محصول را در کادر جستجو وارد کنید",
                    style = Typography.h1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp, start = 4.dp, end = 4.dp),
                )
            }
        }
    }
}