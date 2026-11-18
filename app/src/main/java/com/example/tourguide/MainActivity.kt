package com.example.tourguide

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

class MainActivity : ComponentActivity() {

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startLocationService()
        }
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
                    HomeScreen(
                        onStartTracking = { requestLocationPermissions() }
                    )
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
fun HomeScreen(onStartTracking: () -> Unit) {
    val context = LocalContext.current
    val location by LocationRepository.location.collectAsState()
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var lastPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingPhotoUri != null) {
            // فایل عکس ذخیره شده است؛ مسیر فایل را از content URI استخراج نمی‌کنیم،
            // بلکه فایل را دوباره می‌سازیم
            pendingPhotoUri = null
            lastPhotoFile = lastSavedFile
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "راهنمای کاروان", style = MaterialTheme.typography.headlineSmall)

        if (location != null) {
            val (lat, lon) = location!!
            Text(text = "موقعیت: %.5f , %.5f".format(lat, lon))
        } else {
            Text(text = "در انتظار دریافت موقعیت...")
        }

        OSMComposableMap(
            location = location,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // پیش‌نمایش آخرین عکس (اگر وجود داشته باشد)
        if (lastPhotoFile != null) {
            PhotoThumbnail(lastPhotoFile!!)
        }

        // ردیف دو دکمه: شروع موقعیت و گرفتن عکس
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStartTracking,
                modifier = Modifier.weight(1f)
            ) {
                Text("شروع موقعیت")
            }
            Button(
                onClick = {
                    val photoDir = context.getExternalFilesDir("Pictures")
                        ?: context.filesDir
                    if (!photoDir.exists()) photoDir.mkdirs()
                    val photoFile = File(
                        photoDir,
                        "photo_${System.currentTimeMillis()}.jpg"
                    )
                    lastSavedFile = photoFile
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                    pendingPhotoUri = uri
                    takePictureLauncher.launch(uri)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("📷 گرفتن عکس")
            }
        }

        Button(
            onClick = { showEmergencyDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
        ) {
            Text("🚨 گزارش اضطراری", color = Color.White)
        }

        Spacer(modifier = Modifier.padding(4.dp))
    }

    if (showEmergencyDialog) {
        EmergencyDialog(
            location = location,
            onDismiss = { showEmergencyDialog = false }
        )
    }
}

// متغیر کمکی برای نگهداری فایل ذخیره‌شده (بدون نیاز به وضعیت Compose)
private var lastSavedFile: File? = null

@Composable
fun PhotoThumbnail(file: File) {
    val bitmap = remember(file) {
        try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            null
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "آخرین عکس گرفته شده",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun EmergencyDialog(
    location: Pair<Double, Double>?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نوع مشکل را انتخاب کنید") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        EmergencySmsHelper.sendEmergencySms(context, "خرابی اتوبوس", location)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🚌 خرابی اتوبوس") }

                Button(
                    onClick = {
                        EmergencySmsHelper.sendEmergencySms(context, "گم شدن مسافر", location)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("👤 گم شدن مسافر") }

                Button(
                    onClick = {
                        EmergencySmsHelper.sendEmergencySms(context, "بیماری مسافر", location)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🏥 بیماری مسافر") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("لغو") }
        }
    )
}

@Composable
fun OSMComposableMap(
    location: Pair<Double, Double>?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val openTopoTileSource = remember {
        XYTileSource(
            "OpenTopoMap",
            0, 17, 256, ".png",
            arrayOf(
                "https://a.tile.opentopomap.org/",
                "https://b.tile.opentopomap.org/",
                "https://c.tile.opentopomap.org/"
            ),
            "© OpenTopoMap (CC-BY-SA)"
        )
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(openTopoTileSource)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(35.6892, 51.3890))
        }
    }

    val marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "موقعیت شما"
            icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
        }.also {
            mapView.overlays.add(it)
        }
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
                marker.position = point
                if (!hasCentered) {
                    view.controller.setCenter(point)
                    hasCentered = true
                }
                view.invalidate()
            }
        }
    )
}
