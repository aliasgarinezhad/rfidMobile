package com.jeanwest.mobile.manualRefill

import android.annotation.SuppressLint
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.preference.PreferenceManager
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.android.volley.NoConnectionError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.jeanwest.mobile.MainActivity
import com.jeanwest.mobile.R
import com.jeanwest.mobile.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors

@ExperimentalCoilApi
class AddProductToManualRefillListActivityActivity : ComponentActivity() {

    private var productCode by mutableStateOf("")
    private var uiList = mutableStateListOf<ManualRefillProduct>()
    private var filteredUiList = mutableStateListOf<ManualRefillProduct>()
    private var colorFilterValues = mutableStateListOf("همه رنگ ها")
    private var sizeFilterValues = mutableStateListOf("همه سایز ها")
    private var colorFilterValue by mutableStateOf("همه رنگ ها")
    private var sizeFilterValue by mutableStateOf("همه سایز ها")
    private var storeFilterValue = 0
    private var state = SnackbarHostState()
    private val beep: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private var isCameraOn by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Page()
        }
        loadMemory()
    }

    private fun loadMemory() {

        val type = object : TypeToken<SnapshotStateList<ManualRefillProduct>>() {}.type

        val memory = PreferenceManager.getDefaultSharedPreferences(this)
        storeFilterValue = memory.getInt("userLocationCode", 0)

        productCode = memory.getString("productCodeAddManualRefill", "") ?: ""

        colorFilterValue =
            memory.getString("colorFilterValueAddManualRefill", "همه رنگ ها") ?: "همه رنگ ها"
        sizeFilterValue =
            memory.getString("sizeFilterValueAddManualRefill", "همه سایز ها") ?: "همه سایز ها"

        uiList = Gson().fromJson(
            memory.getString("uiListAddManualRefill", ""),
            type
        ) ?: mutableStateListOf()

        filteredUiList = Gson().fromJson(
            memory.getString("filteredUiListAddManualRefill", ""),
            type
        ) ?: mutableStateListOf()

        sizeFilterValues = Gson().fromJson(
            memory.getString("sizeFilterValuesAddManualRefill", ""),
            sizeFilterValues.javaClass
        ) ?: mutableStateListOf("همه سایز ها")

        colorFilterValues = Gson().fromJson(
            memory.getString("colorFilterValuesAddManualRefill", ""),
            colorFilterValues.javaClass
        ) ?: mutableStateListOf("همه رنگ ها")
    }

    private fun saveToMemory() {

        val memory = PreferenceManager.getDefaultSharedPreferences(this)
        val edit = memory.edit()

        edit.putString("colorFilterValuesAddManualRefill", JSONArray(colorFilterValues).toString())
        edit.putString("sizeFilterValuesAddManualRefill", JSONArray(sizeFilterValues).toString())
        edit.putString("uiListAddManualRefill", Gson().toJson(uiList).toString())
        edit.putString("filteredUiListAddManualRefill", Gson().toJson(filteredUiList).toString())
        edit.putString("productCodeAddManualRefill", productCode)
        edit.putString("colorFilterValueAddManualRefill", colorFilterValue)
        edit.putString("sizeFilterValueAddManualRefill", sizeFilterValue)

        edit.apply()
    }

    private fun filterUiList() {

        val wareHouseFilterOutput = uiList.filter {
            it.wareHouseNumber > 0
        }

        val sizeFilterOutput = if (sizeFilterValue == "همه سایز ها") {
            wareHouseFilterOutput
        } else {
            wareHouseFilterOutput.filter {
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

        uiList.clear()
        filteredUiList.clear()
        colorFilterValues = mutableStateListOf("همه رنگ ها")
        sizeFilterValues = mutableStateListOf("همه سایز ها")

        val url1 =
            "https://rfid-api.avakatan.ir/products/similars?DepartmentInfo_ID=$storeFilterValue&K_Bar_Code=$productCode"

        val request1 = JsonObjectRequest(url1, { response1 ->

            val products = response1.getJSONArray("products")

            if (products.length() > 0) {
                jsonArrayProcess(products)
            } else {

                val url2 =
                    "https://rfid-api.avakatan.ir/products/similars?DepartmentInfo_ID=$storeFilterValue&kbarcode=$productCode"

                val request2 = JsonObjectRequest(url2, { response2 ->

                    val products2 = response2.getJSONArray("products")

                    if (products2.length() > 0) {
                        jsonArrayProcess(products2)
                    }
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
        })

        val queue = Volley.newRequestQueue(this)
        queue.add(request1)
    }

    private fun jsonArrayProcess(similarProductsJsonArray: JSONArray) {

        for (i in 0 until similarProductsJsonArray.length()) {

            val json = similarProductsJsonArray.getJSONObject(i)

            colorFilterValues.add(json.getString("Color"))
            sizeFilterValues.add(json.getString("Size"))

            uiList.add(
                ManualRefillProduct(
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
            )
        }

        colorFilterValues = colorFilterValues.distinct().toMutableStateList()
        sizeFilterValues = sizeFilterValues.distinct().toMutableStateList()

        getScannedProductProperties()
    }

    private fun getScannedProductProperties() {

        val url = "https://rfid-api.avakatan.ir/products/v4"

        val request = object : JsonObjectRequest(Method.POST, url, null, {

            val barcodes = it.getJSONArray("KBarCodes")
            if (barcodes.length() != 0) {
                colorFilterValue = barcodes.getJSONObject(0).getString("Color")
            }
            productCode = uiList[0].productCode
            filterUiList()

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

        }) {
            override fun getHeaders(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/json;charset=UTF-8"
                params["Authorization"] = "Bearer " + MainActivity.token
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


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {

        if (keyCode == 4) {
            back()
        }
        return true
    }

    private fun backToManualRefillActivity(product: ManualRefillProduct) {

        ManualRefillActivity.manualRefillProducts.forEach {
            if (product.KBarCode == it.KBarCode) {
                if (it.requestedNum >= it.wareHouseNumber) {
                    CoroutineScope(Main).launch {
                        state.showSnackbar(
                            "تعداد درخواستی از موجودی انبار بیشتر است.",
                            null,
                            SnackbarDuration.Long,
                        )
                    }
                    return
                } else {
                    it.requestedNum++
                    saveToMemory()
                    finish()
                    return
                }
            }
        }
        saveToMemory()
        product.requestedNum = 1
        ManualRefillActivity.manualRefillProducts.add(product)
        finish()
    }

    private fun back() {
        if (isCameraOn) {
            isCameraOn = false
        } else {
            saveToMemory()
            finish()
        }
    }

    @ExperimentalCoilApi
    @Composable
    fun Page() {
        MyApplicationTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Scaffold(
                    topBar = { AppBar() },
                    content = { Content() },
                    snackbarHost = { ErrorSnackBar(state) },
                )
            }
        }
    }

    @Composable
    fun AppBar() {

        TopAppBar(

            navigationIcon = {
                IconButton(onClick = { back() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = ""
                    )
                }
            },

            title = {
                Text(
                    text = "جست و جو",
                    modifier = Modifier
                        .padding(end = 50.dp)
                        .fillMaxSize()
                        .wrapContentSize(),
                    textAlign = TextAlign.Center,
                )
            }
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @ExperimentalCoilApi
    @Composable
    fun Content() {

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
                    IconButton(
                        onClick = { isCameraOn = true },
                        modifier = Modifier
                            .padding(top = 16.dp, end = 16.dp, bottom = 12.dp)
                            .background(
                                color = MaterialTheme.colors.secondary,
                                shape = MaterialTheme.shapes.small
                            )
                            .size(56.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.barcode_scan_icon),
                            contentDescription = "",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {

                    ColorFilterDropDownList(
                        modifier = Modifier
                            .padding(start = 16.dp, bottom = 16.dp)
                    )
                    SizeFilterDropDownList(
                        modifier = Modifier
                            .padding(start = 16.dp, bottom = 16.dp)
                    )
                }
            }

            if (isCameraOn) {
                CameraPreviewView()
            }

            LazyColumn(modifier = Modifier.padding(top = 0.dp)) {

                items(filteredUiList.size) { i ->
                    LazyColumnItem(i)
                }
            }
        }
    }

    @ExperimentalFoundationApi
    @Composable
    fun LazyColumnItem(i: Int) {

        val topPadding = if(i==0) 16.dp else 0.dp

        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = topPadding)
                .shadow(elevation = 5.dp, shape = MaterialTheme.shapes.small)
                .background(
                    color = MaterialTheme.colors.onPrimary,
                    shape = MaterialTheme.shapes.small
                )
                .fillMaxWidth()
                .height(100.dp)
                .clickable(
                    onClick = {
                        backToManualRefillActivity(filteredUiList[i])
                    },
                )
                .testTag("SearchItems"),
        ) {

            Image(
                painter = rememberImagePainter(
                    filteredUiList[i].imageUrl,
                ),
                contentDescription = "",
                modifier = Modifier
                    .padding(end = 4.dp, top = 12.dp, bottom = 12.dp, start = 12.dp)
                    .shadow(0.dp, shape = Shapes.large)
                    .background(
                        color = MaterialTheme.colors.onPrimary,
                        shape = Shapes.large
                    )
                    .border(
                        BorderStroke(2.dp, color = JeanswestButtonDisabled),
                        shape = Shapes.large
                    )
                    .fillMaxHeight()
                    .width(70.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 8.dp)
            ) {

                Column(
                    modifier = Modifier
                        .weight(1.5F)
                        .fillMaxHeight()
                        .padding(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = filteredUiList[i].size + "-" + filteredUiList[i].color,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Right,
                    )

                    Text(
                        text = filteredUiList[i].name,
                        style = MaterialTheme.typography.h4,
                        textAlign = TextAlign.Right,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1F)
                        .fillMaxHeight()
                        .padding(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {

                    Text(
                        text = "فروشگاه: " + filteredUiList[i].storeNumber,
                        style = MaterialTheme.typography.h3,
                        textAlign = TextAlign.Right,
                    )

                    Text(
                        text = "انبار: " + filteredUiList[i].wareHouseNumber,
                        style = MaterialTheme.typography.h3,
                        textAlign = TextAlign.Right,
                    )
                }
            }
        }
    }

    @Composable
    fun ProductCodeTextField(modifier: Modifier) {

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
    fun SizeFilterDropDownList(modifier: Modifier) {

        var expanded by rememberSaveable {
            mutableStateOf(false)
        }

        Box(
            modifier = modifier
                .shadow(elevation = 1.dp, shape = MaterialTheme.shapes.small)
                .background(
                    color = MaterialTheme.colors.onPrimary,
                    shape = MaterialTheme.shapes.small
                )
                .border(BorderStroke(1.dp, borderColors), shape = MaterialTheme.shapes.small)
                .height(48.dp)
        ) {
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .testTag("SearchSizeFilterDropDownList")
                    .fillMaxHeight(),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.size),
                    contentDescription = "",
                    tint = iconColors,
                    modifier = Modifier
                        .size(28.dp)
                        .align(CenterVertically)
                        .padding(start = 4.dp)
                )
                Text(
                    text = sizeFilterValue,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .align(CenterVertically)
                        .padding(start = 6.dp)
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    "",
                    modifier = Modifier
                        .align(CenterVertically)
                        .padding(start = 4.dp, end = 4.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.wrapContentWidth()
            ) {
                sizeFilterValues.forEach {
                    DropdownMenuItem(onClick = {
                        expanded = false
                        sizeFilterValue = it
                        filterUiList()
                    }) {
                        Text(text = it)
                    }
                }
            }
        }
    }

    @Composable
    fun ColorFilterDropDownList(modifier: Modifier) {

        var expanded by rememberSaveable {
            mutableStateOf(false)
        }

        Box(
            modifier = modifier
                .shadow(elevation = 1.dp, shape = MaterialTheme.shapes.small)
                .background(
                    color = MaterialTheme.colors.onPrimary,
                    shape = MaterialTheme.shapes.small
                )
                .border(BorderStroke(1.dp, borderColors), shape = MaterialTheme.shapes.small)
                .height(48.dp)
        ) {
            Row(modifier = Modifier
                .clickable { expanded = true }
                .testTag("SearchColorFilterDropDownList")
                .fillMaxHeight()) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_color_lens_24),
                    contentDescription = "",
                    tint = iconColors,
                    modifier = Modifier
                        .align(CenterVertically)
                        .padding(start = 4.dp)
                )
                Text(
                    style = MaterialTheme.typography.body2,
                    text = colorFilterValue,
                    modifier = Modifier
                        .align(CenterVertically)
                        .padding(start = 4.dp)
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    "",
                    modifier = Modifier
                        .align(CenterVertically)
                        .padding(start = 4.dp, end = 4.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.wrapContentWidth()
            ) {

                colorFilterValues.forEach {
                    DropdownMenuItem(onClick = {
                        expanded = false
                        colorFilterValue = it
                        filterUiList()
                    }) {
                        Text(text = it)
                    }
                }
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Composable
    private fun CameraPreviewView() {

        val cameraProvider = ProcessCameraProvider.getInstance(this).get()
        val previewView = PreviewView(this)
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128
            )
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->

            if (isCameraOn) {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {

                    val image =
                        InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    val scanner = BarcodeScanning.getClient(options)
                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                productCode = barcode.displayValue.toString()
                                isCameraOn = false
                                beep.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                                getSimilarProducts()
                            }
                            imageProxy.close()
                        }
                        .addOnFailureListener {
                            imageProxy.close()
                        }
                }
            } else {
                imageProxy.close()
            }
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            this,
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build(),
            imageAnalysis,
            preview
        )

        Box(
            modifier = Modifier
                .padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
                .fillMaxSize()
        ) {
            AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        }
    }
}