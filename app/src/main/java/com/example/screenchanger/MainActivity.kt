package com.example.screenchanger

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private var selectedRatio by mutableStateOf("")
    private var statusMessage by mutableStateOf("")
    private var pendingOperation: String? = null
    private var ratioService: IDisplayRatioService? = null
    private var serviceBindingRequested = false
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == PERMISSION_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    bindRatioService()
                } else {
                    pendingOperation = null
                    statusMessage = "Доступ Shizuku не надано"
                }
            }
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            ratioService = IDisplayRatioService.Stub.asInterface(service)
            serviceBindingRequested = true
            dispatchPendingOperation()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            ratioService = null
            serviceBindingRequested = false
            statusMessage = "З'єднання з Shizuku втрачено"
        }
    }

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(packageName, ScreenRatioUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("screen-ratio")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        selectedRatio = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .getString(SELECTED_RATIO_KEY, "")
            .orEmpty()
        if (selectedRatio.isNotEmpty()) {
            statusMessage = "Натисни вибраний режим ще раз, щоб відновити роздільність"
        }
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        setContent {
            ScreenChangerApp(
                selectedRatio = selectedRatio,
                statusMessage = statusMessage,
                onRatioSelected = ::selectRatio
            )
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        if (serviceBindingRequested) {
            runCatching {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            }
        }
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun selectRatio(ratio: String) {
        pendingOperation = if (selectedRatio == ratio) RESET_OPERATION else ratio
        if (!Shizuku.pingBinder()) {
            statusMessage = "Запусти Shizuku через налагодження по Wi-Fi"
            return
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            statusMessage = "Підтверди запит доступу у Shizuku"
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            return
        }
        bindRatioService()
    }

    private fun bindRatioService() {
        if (ratioService != null) {
            dispatchPendingOperation()
            return
        }
        if (serviceBindingRequested) return

        try {
            statusMessage = "Підключення..."
            serviceBindingRequested = true
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (exception: Exception) {
            serviceBindingRequested = false
            pendingOperation = null
            statusMessage = exception.message ?: "Не вдалося підключитися до Shizuku"
        }
    }

    private fun dispatchPendingOperation() {
        val operation = pendingOperation ?: return
        val service = ratioService ?: return
        pendingOperation = null
        statusMessage = "Застосовую режим $operation..."

        executor.execute {
            val result = runCatching {
                if (operation == RESET_OPERATION) {
                    service.resetResolution()
                } else {
                    service.applyRatio(operation)
                }
            }.getOrElse { "ERROR|${it.message ?: "Помилка Shizuku"}" }

            mainHandler.post {
                if (result.startsWith("OK|")) {
                    if (operation == RESET_OPERATION) {
                        selectedRatio = ""
                        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
                            .edit()
                            .remove(SELECTED_RATIO_KEY)
                            .apply()
                        statusMessage = "Відновлено початкову роздільність"
                    } else {
                        selectedRatio = operation
                        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
                            .edit()
                            .putString(SELECTED_RATIO_KEY, operation)
                            .apply()
                        statusMessage = "Режим $operation застосовано (${result.substringAfter('|')})"
                    }
                } else {
                    statusMessage = result.removePrefix("ERROR|")
                }
            }
        }
    }

    private companion object {
        const val PERMISSION_REQUEST_CODE = 417
        const val RESET_OPERATION = "RESET"
        const val PREFERENCES_NAME = "screen_ratio"
        const val SELECTED_RATIO_KEY = "selected_ratio"
    }
}

@androidx.compose.runtime.Composable
private fun ScreenChangerApp(
    selectedRatio: String,
    statusMessage: String,
    onRatioSelected: (String) -> Unit
) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101B18))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RatioButton("16:9", selectedRatio == "16:9") { onRatioSelected("16:9") }
            RatioButton("4:3", selectedRatio == "4:3") { onRatioSelected("4:3") }
            if (statusMessage.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = statusMessage,
                    color = Color(0xFFB7C8BF),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun RatioButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF42A17D) else Color(0xFF263A32),
            contentColor = Color.White
        )
    ) {
        Text(label, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}