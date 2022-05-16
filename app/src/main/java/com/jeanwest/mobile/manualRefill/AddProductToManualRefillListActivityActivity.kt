package com.jeanwest.mobile.manualRefill

//import com.jeanwest.reader.hardware.Barcode2D
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.android.volley.NoConnectionError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeanwest.mobile.R
import com.jeanwest.mobile.hardware.Barcode2D
import com.jeanwest.mobile.hardware.IBarcodeResult
import com.jeanwest.mobile.theme.ErrorSnackBar
import com.jeanwest.mobile.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import org.json.JSONArray

@ExperimentalCoilApi
class AddProductToManualRefillListActivityActivity : ComponentActivity(), IBarcodeResult {

    private var productCode by mutableStateOf("")
    private var uiList by mutableStateOf(mutableListOf<ManualRefillProduct>())
    private var filteredUiList by mutableStateOf(mutableListOf<ManualRefillProduct>())
    private var colorFilterValues by mutableStateOf(mutableListOf("همه رنگ ها"))
    private var sizeFilterValues by mutableStateOf(mutableListOf("همه سایز ها"))
    private var barcode2D = Barcode2D(this)

    private var colorFilterValue by mutableStateOf("همه رنگ ها")
    private var sizeFilterValue by mutableStateOf("همه سایز ها")
    private var storeFilterValue = 0
    private var state = SnackbarHostState()
    private val beep: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Page()
        }
        open()
        loadMemory()
    }

    private fun loadMemory() {

        val type = object : TypeToken<List<ManualRefillProduct>>() {}.type

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
        ) ?: mutableListOf()

        filteredUiList = Gson().fromJson(
            memory.getString("filteredUiListAddManualRefill", ""),
            type
        ) ?: mutableListOf()

        sizeFilterValues = Gson().fromJson(
            memory.getString("sizeFilterValuesAddManualRefill", ""),
            sizeFilterValues.javaClass
        ) ?: mutableListOf("همه سایز ها")

        colorFilterValues = Gson().fromJson(
            memory.getString("colorFilterValuesAddManualRefill", ""),
            colorFilterValues.javaClass
        ) ?: mutableListOf("همه رنگ ها")

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

    private fun filterUiList(uiList: MutableList<ManualRefillProduct>): MutableList<ManualRefillProduct> {

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

        return colorFilterOutput.toMutableList()
    }

    private fun getSimilarProducts() {

        uiList = mutableListOf()
        filteredUiList = mutableListOf()
        colorFilterValues = mutableListOf("همه رنگ ها")
        sizeFilterValues = mutableListOf("همه سایز ها")

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
                    scannedEPCs = mutableListOf(),
                    scannedBarcode = "",
                    scannedEPCNumber = 0,
                    scannedBarcodeNumber = 0,
                    kName = json.getString("K_Name"),
                    requestedNum = 0,
                    storeNumber = json.getInt("dbCountStore"),
                )
            )
        }

        productCode = uiList[0].productCode
        colorFilterValues = colorFilterValues.distinct().toMutableList()
        sizeFilterValues = sizeFilterValues.distinct().toMutableList()
        filteredUiList = filterUiList(uiList)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {

        if (keyCode == 280 || keyCode == 139 || keyCode == 293) {
            start()
        } else if (keyCode == 4) {
            back()
        }
        return true
    }

    private fun start() {
        barcode2D.startScan(this)
    }

    private fun open() {
        barcode2D.open(this, this)
    }

    private fun close() {
        barcode2D.stopScan(this)
        barcode2D.close(this)
    }

    override fun getBarcode(barcode: String?) {
        if (!barcode.isNullOrEmpty()) {
            beep.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
            productCode = barcode
            getSimilarProducts()
        }
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
        saveToMemory()
        close()
        finish()
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
            title = {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 0.dp, end = 50.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "اضافه کردن کالای جدید", textAlign = TextAlign.Center,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { back() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = ""
                    )
                }
            }
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @ExperimentalCoilApi
    @Composable
    fun Content() {

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .padding(start = 5.dp, end = 5.dp, bottom = 5.dp, top = 5.dp)
                    .background(
                        color = MaterialTheme.colors.onPrimary,
                        shape = MaterialTheme.shapes.small
                    )
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column {
                    ProductCodeTextField()
                    Row(
                        modifier = Modifier
                            .padding(bottom = 5.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ColorFilterDropDownList()
                        SizeFilterDropDownList()
                    }
                }
            }

            LazyColumn(modifier = Modifier.padding(top = 2.dp)) {

                items(filteredUiList.size) { i ->
                    LazyColumnItem(i)
                }
            }
        }
    }

    @ExperimentalFoundationApi
    @Composable
    fun LazyColumnItem(i: Int) {

        Row(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                .background(
                    color = MaterialTheme.colors.onPrimary,
                    shape = MaterialTheme.shapes.small
                )
                .fillMaxWidth()
                .height(80.dp)
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
                    .fillMaxHeight()
                    .width(80.dp)
                    .padding(vertical = 8.dp, horizontal = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 8.dp)
            ) {

                Column(
                    modifier = Modifier
                        .weight(1F)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = filteredUiList[i].name,
                        style = MaterialTheme.typography.h1,
                        textAlign = TextAlign.Right,
                    )

                    Text(
                        text = filteredUiList[i].size + "-" +  filteredUiList[i].color,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Right,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1.2F)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {

                    Text(
                        text = "موجودی فروشگاه: " + filteredUiList[i].storeNumber,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Right,
                    )

                    Text(
                        text = "موجودی انبار: " + filteredUiList[i].wareHouseNumber,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Right,
                    )
                }
            }
        }
    }

    @Composable
    fun ProductCodeTextField() {

        OutlinedTextField(
            value = productCode, onValueChange = {
                productCode = it
            },
            modifier = Modifier
                .padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 10.dp)
                .fillMaxWidth()
                .testTag("SearchProductCodeTextField"),
            label = { Text(text = "کد محصول") },
            keyboardActions = KeyboardActions(onSearch = { getSimilarProducts() }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
    }

    @Composable
    fun SizeFilterDropDownList() {

        var expanded by rememberSaveable {
            mutableStateOf(false)
        }

        Box {
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .testTag("SearchSizeFilterDropDownList"),
            ) {
                Text(text = sizeFilterValue)
                Icon(imageVector = Icons.Filled.ArrowDropDown, "")
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
                        filteredUiList = filterUiList(uiList)
                    }) {
                        Text(text = it)
                    }
                }
            }
        }
    }

    @Composable
    fun ColorFilterDropDownList() {

        var expanded by rememberSaveable {
            mutableStateOf(false)
        }

        Box {
            Row(modifier = Modifier
                .clickable { expanded = true }
                .testTag("SearchColorFilterDropDownList")) {
                Text(text = colorFilterValue)
                Icon(imageVector = Icons.Filled.ArrowDropDown, "")
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
                        filteredUiList = filterUiList(uiList)
                    }) {
                        Text(text = it)
                    }
                }
            }
        }
    }
}