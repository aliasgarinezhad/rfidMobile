package com.jeanwest.mobile

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import coil.annotation.ExperimentalCoilApi
import com.jeanwest.mobile.iotHub.IotHub
import com.jeanwest.mobile.logIn.OperatorLoginActivity
import com.jeanwest.mobile.logIn.UserLoginActivity
import com.jeanwest.mobile.manualRefill.ManualRefillActivity
import com.jeanwest.mobile.theme.ErrorSnackBar
import com.jeanwest.mobile.theme.MyApplicationTheme
import java.io.File


@ExperimentalCoilApi
class MainActivity : ComponentActivity() {

    private var openAccountDialog by mutableStateOf(false)
    private lateinit var memory: SharedPreferences
    private var deviceId = ""
    private var buttonSize = 100.dp
    private var iconSize = 48.dp
    private var pageSize = buttonSize * 3 + 120.dp
    private var userLocationCode = 0
    private var fullName = ""
    private var state = SnackbarHostState()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        clearCash()

        Intent(this, IotHub::class.java).also { intent ->
            startService(intent)
        }

        memory = PreferenceManager.getDefaultSharedPreferences(this)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                0
            )
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                1
            )
        }

        loadMemory()
    }

    private fun loadMemory() {

        memory = PreferenceManager.getDefaultSharedPreferences(this)

        userLocationCode = memory.getInt("userLocationCode", 0)
        username = memory.getString("username", "") ?: ""
        token = memory.getString("accessToken", "") ?: ""
        deviceId = memory.getString("deviceId", "") ?: ""
        fullName = memory.getString("userFullName", "") ?: ""
    }

    override fun onResume() {
        super.onResume()
        setContent {
            MainMenu()
        }

        if (deviceId == "") {
            val intent =
                Intent(this@MainActivity, OperatorLoginActivity::class.java)
            intent.flags += Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else if (username == "") {
            val intent =
                Intent(this@MainActivity, UserLoginActivity::class.java)
            intent.flags += Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        val intent =
            Intent(this@MainActivity, ManualRefillActivity::class.java)
        intent.flags += Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == 4) {
            finish()
        }
        return true
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

    companion object {

        var username = ""
        var token = ""
    }

    @Composable
    fun MainMenu() {

        MyApplicationTheme {

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    modifier = Modifier
                                        .padding(start = 40.dp)
                                        .height(16.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_jeanswest_logo),
                                        contentDescription = "",
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    openAccountDialog = true
                                })
                                {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_baseline_person_24),
                                        contentDescription = "",
                                    )
                                }
                            }
                        )
                    },
                    content = {

                        Column(
                            verticalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .height(buttonSize * 1 + 50.dp)
                                .width(pageSize),
                        ) {

                            if (openAccountDialog) {
                                AccountAlertDialog()
                            }

                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ManualRefillWarehouseManagerButton()
                                Box(modifier = Modifier.size(100.dp)) {}
                                Box(modifier = Modifier.size(100.dp)) {}
                            }
                        }
                    },
                    snackbarHost = { ErrorSnackBar(state) },
                )
            }
        }
    }

    @Composable
    fun ManualRefillWarehouseManagerButton() {

        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .size(buttonSize)
                .clickable {
                    val intent = Intent(this, ManualRefillActivity::class.java)
                    startActivity(intent)
                }
                .background(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colors.onPrimary,
                )
        ) {

            Icon(
                painter = painterResource(R.drawable.check_in),
                contentDescription = "",
                tint = MaterialTheme.colors.primary,
                modifier = Modifier
                    .size(iconSize)
                    .align(Alignment.CenterHorizontally)
            )
            Text(
                stringResource(id = R.string.manualRefill),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h3,
            )
        }
    }

    @Composable
    fun AccountAlertDialog() {

        AlertDialog(
            onDismissRequest = {
                openAccountDialog = false
            },
            buttons = {

                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .height(120.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = fullName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Button(
                            onClick = {
                                openAccountDialog = false
                                val editor: SharedPreferences.Editor = memory.edit()
                                editor.putString("accessToken", "")
                                editor.putString("username", "")
                                editor.apply()
                                username = ""
                                token = ""
                                val intent =
                                    Intent(this@MainActivity, UserLoginActivity::class.java)
                                intent.flags += Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            },
                        ) {
                            Text(text = "خروج از حساب")
                        }
                    }
                }
            }
        )
    }
}