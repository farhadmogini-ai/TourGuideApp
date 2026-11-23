package com.example.tourguide

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : ComponentActivity() {

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocation) startLocationService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(
            applicationContext,
            applicationContext.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    var user by remember { mutableStateOf(SessionManager.getUser(context)) }

                    if (user == null) {
                        LoginScreen(serverUrl = AppConfig.SERVER_URL,
                            onLoginSuccess = { u -> user = u })
                    } else {
                        HomeScreen(
                            user = user!!,
                            onStartTracking = { requestLocationPermissions() },
                            onLogout = {
                                stopService(Intent(this@MainActivity, LocationService::class.java))
                                SessionManager.clear(context)
                                user = null
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        locationPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}

@Composable
fun LoginScreen(serverUrl: String, onLoginSuccess: (User) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🚌 راهنمای کاروان", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(text = "ورود راهنما", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("نام کاربری (کد ملی)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("رمز عبور") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = error!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    error = "نام کاربری و رمز را وارد کنید"
                    return@Button
                }
                error = null
                loading = true
                scope.launch {
                    when (val result = ApiClient.login(serverUrl, username.trim(), password)) {
                        is LoginResult.Success -> {
                            loading = false
                            if (result.user.role != "guide") {
                                error = "این اپ مخصوص راهنماست"
                            } else {
                                SessionManager.save(context, result.token, result.user)
                                onLoginSuccess(result.user)
                            }
                        }
                        is LoginResult.Failure -> {
                            loading = false
                            error = result.message
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) {
            if (loading) CircularProgressIndicator(
                modifier = Modifier.height(20.dp), color = Color.White
            )
            else Text("ورود")
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "سرور: $serverUrl",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
@Composable
fun HomeScreen(user: User, onStartTracking: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val location by LocationRepository.location.collectAsState()
    val serverStatus by LocationRepository.serverStatus.collectAsState()
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showRoutingDialog by remember { mutableStateOf(false) }
    var stations by remember { mutableStateOf<List<Station>>(emptyList()) }

    LaunchedEffect(user) {
        val token = SessionManager.getToken(context)
        if (token != null) {
            val list = ApiClient.getStations(AppConfig.SERVER_URL, token)
            if (list != null) stations = list
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🚌 ${user.fullName}", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "اتوبوس ${user.busNumber ?: "-"} • ${user.city ?: "-"}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "وضعیت سرور: $serverStatus",
            style = MaterialTheme.typography.bodySmall,
            color = if (serverStatus.startsWith("✅")) Color(0xFF2E7D32) else Color(0xFF757575)
        )

        if (location != null) {
            val (lat, lon) = location!!
            Text(text = "موقعیت: %.5f , %.5f".format(lat, lon), style = MaterialTheme.typography.bodySmall)
        }

        OSMComposableMap(
            location = location,
            stations = stations,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(onClick = onStartTracking, modifier = Modifier.weight(1f)) {
                Text("شروع موقعیت")
            }
            Button(onClick = { showRoutingDialog = true }, modifier = Modifier.weight(1f)) {
                Text("🗺️ مسیریابی")
            }
        }

        Button(
            onClick = { showEmergencyDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
        ) {
            Text("🚨 گزارش اضطراری", color = Color.White)
        }

        TextButton(onClick = onLogout) { Text("خروج از حساب") }
    }

    if (showEmergencyDialog) {
        EmergencyDialog(location = location, onDismiss = { showEmergencyDialog = false })
    }
    if (showRoutingDialog) {
        RoutingDialog(location = location, stations = stations, onDismiss = { showRoutingDialog = false })
    }
}

@Composable
fun RoutingDialog(
    location: Pair<Double, Double>?,
    stations: List<Station>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sorted = remember(location, stations) {
        if (location != null) {
            stations.map { it to StationHelper.distanceKm(location.first, location.second, it.latitude, it.longitude) }
                .sortedBy { it.second }
        } else stations.map { it to null }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ایستگاه‌های مسیر") },
        text = {
            if (location == null) {
                Text("ابتدا «شروع موقعیت» را بزنید.")
            } else if (stations.isEmpty()) {
                Text("هنوز ایستگاهی تعریف نشده. با مدیر تماس بگیرید.")
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sorted.forEach { (station, distance) ->
                        if (distance != null) {
                            val minutes = StationHelper.estimateMinutes(distance)
                            Button(
                                onClick = {
                                    StationHelper.openNavigation(context, station)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                    Text(station.name, style = MaterialTheme.typography.bodyLarge)
                                    Text("%.1f کیلومتر • ~%d دقیقه".format(distance, minutes),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
fun EmergencyDialog(location: Pair<Double, Double>?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نوع مشکل را انتخاب کنید") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("خرابی اتوبوس", "گم شدن مسافر", "بیماری مسافر").forEach { type ->
                    Button(onClick = {
                        EmergencySmsHelper.sendEmergencySms(context, type, location)
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth()) { Text(type) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("لغو") } }
    )
}

@Composable
fun OSMComposableMap(
    location: Pair<Double, Double>?,
    stations: List<Station>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val tileSource = remember {
        XYTileSource("OpenTopoMap", 0, 17, 256, ".png",
            arrayOf(
                "https://a.tile.opentopomap.org/",
                "https://b.tile.opentopomap.org/",
                "https://c.tile.opentopomap.org/"
            ), "© OpenTopoMap (CC-BY-SA)")
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(tileSource)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(35.6892, 51.3890))
        }
    }

    val userMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "موقعیت شما"
            icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
        }.also { mapView.overlays.add(it) }
    }

    var stationMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    LaunchedEffect(stations) {
        stationMarkers.forEach { mapView.overlays.remove(it) }
        val newMarkers = stations.map { s ->
            Marker(mapView).apply {
                position = GeoPoint(s.latitude, s.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = s.name
                icon = ContextCompat.getDrawable(context, android.R.drawable.star_big_on)
            }.also { mapView.overlays.add(it) }
        }
        stationMarkers = newMarkers
        mapView.invalidate()
    }

    var hasCentered by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            if (location != null) {
                val point = GeoPoint(location.first, location.second)
                userMarker.position = point
                if (!hasCentered) {
                    view.controller.setCenter(point)
                    hasCentered = true
                }
                view.invalidate()
            }
        }
    )
}
