package com.jeanwest.mobile.manualRefill


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.android.volley.NoConnectionError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeanwest.mobile.MainActivity
import com.jeanwest.mobile.R
import com.jeanwest.mobile.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalFoundationApi::class)
class ManualRefillActivity : ComponentActivity() {

    //ui parameters
    private var isDataLoading by mutableStateOf(false)
    private var openClearDialog by mutableStateOf(false)
    var selectedProductCodes = mutableListOf<String>()
    private var state = SnackbarHostState()
    var uiList = mutableStateListOf<ManualRefillProduct>()
    private var selectMode by mutableStateOf(false)
    private var sumOfManualRefill by mutableStateOf(0)
    private var isSubmitting by mutableStateOf(false)


    companion object {
        var manualRefillProducts = ArrayList<ManualRefillProduct>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Page()
        }

        loadMemory()
    }

    override fun onResume() {
        super.onResume()
        uiList.clear()
        uiList.addAll(manualRefillProducts)
        sumOfManualRefill = 0
        uiList.forEach {
            sumOfManualRefill += it.requestedNum
        }
        saveToMemory()
    }

    private fun sendManualRefillRequest() {

        if (sumOfManualRefill == 0) {
            CoroutineScope(Dispatchers.Default).launch {
                state.showSnackbar(
                    "لیست شارژ خالی است.",
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
                    "Bearer " + MainActivity.token
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

        manualRefillProducts = Gson().fromJson(
            memory.getString("ManualRefillProducts", ""),
            type
        ) ?: ArrayList()
    }

    fun clear() {

        val removedRefillProducts = mutableListOf<ManualRefillProduct>()

        manualRefillProducts.forEach {
            if (it.KBarCode in selectedProductCodes) {
                removedRefillProducts.add(it)
            }
        }
        manualRefillProducts.removeAll(removedRefillProducts)
        removedRefillProducts.clear()

        uiList.clear()
        uiList.addAll(manualRefillProducts)

        sumOfManualRefill = 0
        uiList.forEach {
            sumOfManualRefill += it.requestedNum
        }
        selectMode = false
        selectedProductCodes = mutableListOf()
        saveToMemory()
    }

    private fun back() {

        if (selectMode) {
            selectedProductCodes = mutableListOf()
            selectMode = false
            uiList.clear()
            uiList.addAll(manualRefillProducts)
        } else {
            saveToMemory()
            finish()
        }
    }

    @ExperimentalCoilApi
    @ExperimentalFoundationApi
    @Composable
    fun Page() {
        MyApplicationTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Scaffold(
                    topBar = { AppBar() },
                    bottomBar = { if (selectMode) SelectedBottomAppBar() else BottomBar() },
                    content = { Content() },
                    snackbarHost = { ErrorSnackBar(state) },
                )
            }
        }
    }

    @Composable
    fun SelectedBottomAppBar() {
        BottomAppBar(
            backgroundColor = JeanswestBottomBar,
            modifier = Modifier
                .wrapContentHeight()
                .background(shape = MaterialTheme.shapes.large, color = JeanswestBottomBar),
        ) {

            Column {

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = {
                        uiList.forEach {
                            selectedProductCodes.add(it.KBarCode)
                        }
                        uiList.clear()
                        uiList.addAll(manualRefillProducts)
                    }) {
                        Text(text = "انتخاب همه")
                    }
                    Button(onClick = {
                        openClearDialog = true
                    }) {
                        Text(text = "پاک کردن")
                    }
                    Button(onClick = {
                        selectedProductCodes.clear()
                        selectMode = false
                        uiList.clear()
                        uiList.addAll(manualRefillProducts)
                    }) {
                        Text(text = "بازگشت")
                    }
                }
            }
        }
    }

    @Composable
    fun BottomBar() {
        BottomAppBar(
            backgroundColor = JeanswestBottomBar,
            modifier = Modifier
                .wrapContentHeight(),
            //.background(shape = MaterialTheme.shapes.large, color = JeanswestBottomBar),
        ) {

            Column {

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "درخواست شارژ " + "(" + sumOfManualRefill + ")",
                        textAlign = TextAlign.Right,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    )

                    Button(onClick = {
                        Intent(
                            this@ManualRefillActivity,
                            AddProductToManualRefillListActivityActivity::class.java
                        ).apply {
                            startActivity(this)
                        }
                    }) {
                        Text(text = "تعریف شارژ")
                    }

                    Button(onClick = {
                        if(!isSubmitting) {
                            sendManualRefillRequest()
                        }
                    }) {
                        if(isSubmitting) {
                            Text(text = "در حال ثبت ...")
                        } else {
                            Text(text = "ثبت شارژ")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AppBar() {

        TopAppBar(

            /*navigationIcon = {
                IconButton(onClick = { back() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = ""
                    )
                }
            },
*/
            title = {
                Text(
                    text = "شارژ",
                    modifier = Modifier
                        //.padding(end = 50.dp)
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

            if (openClearDialog) {
                ClearAlertDialog()
            }

            Column(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 0.dp)
                    .background(
                        MaterialTheme.colors.onPrimary,
                        shape = MaterialTheme.shapes.small
                    )
                    .fillMaxWidth()
            ) {

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
            }

            LazyColumn(modifier = Modifier.padding(top = 2.dp, bottom = 56.dp)) {

                items(uiList.size) { i ->
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
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = topPadding)
                .shadow(elevation = 5.dp, shape = MaterialTheme.shapes.small)
                .background(
                    color = MaterialTheme.colors.onPrimary,
                    shape = MaterialTheme.shapes.small
                )

                .fillMaxWidth()
                .height(100.dp)
                .combinedClickable(
                    onClick = {
                        if (selectMode) {
                            if (uiList[i].KBarCode !in selectedProductCodes) {
                                selectedProductCodes.add(uiList[i].KBarCode)
                            } else {
                                selectedProductCodes.remove(uiList[i].KBarCode)
                                if (selectedProductCodes.size == 0) {
                                    selectMode = false
                                }
                            }
                            uiList.clear()
                            uiList.addAll(manualRefillProducts)
                        }
                    },
                    onLongClick = {
                        selectMode = true
                        if (uiList[i].KBarCode !in selectedProductCodes) {
                            selectedProductCodes.add(uiList[i].KBarCode)
                        } else {
                            selectedProductCodes.remove(uiList[i].KBarCode)
                            if (selectedProductCodes.size == 0) {
                                selectMode = false
                            }
                        }
                        uiList.clear()
                        uiList.addAll(manualRefillProducts)
                    },
                )
                .testTag("refillItems"),
        ) {

            if (uiList[i].KBarCode in selectedProductCodes) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_check_circle_24),
                    tint = doneColor,
                    contentDescription = "",
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterVertically)
                )
            }

            Image(
                painter = rememberImagePainter(
                    uiList[i].imageUrl,
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
                    .padding(start = 8.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.5F)
                        .fillMaxHeight()
                        .padding(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {

                    Text(
                        text = uiList[i].KBarCode,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Right,
                    )
                    Text(
                        text = uiList[i].name,
                        style = MaterialTheme.typography.h4,
                        textAlign = TextAlign.Right,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1F)
                        .fillMaxHeight()
                        .padding(top = 16.dp, bottom = 16.dp),

                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "درخواستی: " + uiList[i].requestedNum,
                        style = MaterialTheme.typography.h3,
                        textAlign = TextAlign.Right,
                    )
                    Text(
                        text = "انبار: " + uiList[i].wareHouseNumber.toString(),
                        style = MaterialTheme.typography.h3,
                        textAlign = TextAlign.Right,
                    )
                }
            }
        }
    }

    @Composable
    fun ClearAlertDialog() {

        AlertDialog(
            onDismissRequest = {
                openClearDialog = false
            },
            buttons = {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceAround
                ) {

                    Text(
                        text = "شارژهایی که علامت زده اید پاک شوند؟",
                        modifier = Modifier.padding(bottom = 10.dp),
                        fontSize = 18.sp
                    )

                    Row(horizontalArrangement = Arrangement.SpaceAround) {

                        Button(onClick = {
                            openClearDialog = false
                            clear()

                        }, modifier = Modifier.padding(top = 10.dp, end = 20.dp)) {
                            Text(text = "بله")
                        }
                        Button(
                            onClick = { openClearDialog = false },
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            Text(text = "خیر")
                        }
                    }
                }
            }
        )
    }
}