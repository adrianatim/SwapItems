package com.example.firstproject

import android.Manifest
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.longdo.mjpegviewer.MjpegView

private const val videoUrl = "http://pendelcam.kip.uni-heidelberg.de/mjpg/video.mjpg"

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted
            setContent {
                ScreenView()
            }
        } else {
            // Permission is denied
            setContent {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PermissionDeniedView()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

@Composable
fun ScreenView() {

    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    var swap by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val widthInDp = configuration.screenWidthDp.dp

    val mpeg = remember(context, videoUrl) {
        MjpegView(context).apply {
            mode = MjpegView.MODE_FIT_WIDTH
            isAdjustHeight = true
            supportPinchZoomAndPan = true
            msecWaitAfterReadImageError = 0
            setUrl(videoUrl)
            startStream()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!swap) {
                // Preview Camera
                CameraPreviewHalfScreen(
                    Modifier
                        .width((widthInDp / 2) - 10.dp)
                        .height(200.dp),
                    lifecycle
                )
            }
            //Streaming Video
            VideoStreaming(
                mpeg,
                Modifier
                    .width((widthInDp / 2) - 10.dp)
                    .height(200.dp)
            )
            if (swap) {
                // Preview Camera
                CameraPreviewHalfScreen(
                    Modifier
                        .width((widthInDp / 2) - 10.dp)
                        .height(200.dp),
                    lifecycle
                )
            }
        }
        Spacer(modifier = Modifier.size(200.dp))
        Row {
            Button(
                modifier = Modifier.height(40.dp),
                onClick = {
                    swap = !swap
                }) {
                Text("Swap")
            }
        }
    }
}

@Composable
fun VideoStreaming(mpeg: MjpegView, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = Color.White
        )
        AndroidView(
            modifier = modifier,
            factory = { _ -> mpeg }
        )
    }
}

@Composable
fun CameraPreviewHalfScreen(modifier: Modifier, lifecycle: LifecycleOwner) {
    val context = LocalContext.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { ContextCompat.getMainExecutor(context) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider: ProcessCameraProvider =
                        cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycle,
                            cameraSelector,
                            preview
                        )
                    } catch (_: Exception) {
                    }
                }, executor)

                previewView
            },
            modifier = modifier
        )
    }
}

@Composable
fun PermissionDeniedView() {
    Text(
        text = "Camera permission is denied. Please grant permission from Settings in order to use" +
                " the app.\n Thank you!",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 50.dp),
        textAlign = TextAlign.Center,
        fontSize = 16.sp
    )
}
