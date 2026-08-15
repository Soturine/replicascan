package com.soturine.scanora.feature.camera

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.soturine.scanora.core.common.model.DocumentDetectionConfidence
import com.soturine.scanora.core.common.model.DocumentQuad
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCaptureScreen(
    state: CameraCaptureUiState,
    onPermissionResult: (Boolean) -> Unit,
    onCapturedImage: (String) -> Unit,
    onAnalyzeFrame: (IntArray, Int, Int) -> Unit,
    onDone: (List<String>) -> Unit,
    onBack: () -> Unit,
    onCaptureStarted: () -> Boolean,
    onCaptureFinished: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val captureFailedMessage = stringResource(id = R.string.camera_capture_failed)
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult,
    )
    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.camera_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.camera_back))
                    }
                },
                actions = {
                    if (state.permissionGranted && camera?.cameraInfo?.hasFlashUnit() == true) {
                        IconButton(onClick = {
                            torchEnabled = !torchEnabled
                            camera?.cameraControl?.enableTorch(torchEnabled)
                        }) {
                            Icon(
                                if (torchEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                                stringResource(if (torchEnabled) R.string.camera_torch_disable else R.string.camera_torch_enable),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state.permissionGranted) {
            Box(Modifier.fillMaxSize().padding(innerPadding).background(Color.Black)) {
                CameraPreview(
                    imageCapture = imageCapture,
                    lifecycleOwner = lifecycleOwner,
                    onFrame = onAnalyzeFrame,
                    onCameraReady = { camera = it },
                    onError = { onError(captureFailedMessage) },
                    modifier = Modifier.fillMaxSize(),
                )
                LiveDocumentOverlay(state.liveQuad, state.liveConfidence, Modifier.fillMaxSize())
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                    color = Color.Black.copy(alpha = 0.66f),
                    contentColor = Color.White,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(
                        stringResource(if (state.liveQuad == null) R.string.camera_status_searching else R.string.camera_status_detected),
                        Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.78f),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        CapturedPageIndicator(state.capturedUris.lastOrNull(), state.capturedUris.size)
                        FilledIconButton(
                            modifier = Modifier.size(76.dp).border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            enabled = !state.isCapturing,
                            onClick = {
                                if (!onCaptureStarted()) return@FilledIconButton
                                capturePhoto(
                                    context,
                                    imageCapture,
                                    onCapturedImage,
                                    onFailure = {
                                        onCaptureFinished()
                                        onError(captureFailedMessage)
                                    },
                                )
                            },
                        ) {
                            if (state.isCapturing) CircularProgressIndicator(Modifier.size(28.dp))
                            else Icon(Icons.Outlined.CameraAlt, stringResource(R.string.camera_take_photo), Modifier.size(30.dp))
                        }
                        FilledTonalButton(
                            enabled = state.capturedUris.isNotEmpty() && !state.isCapturing,
                            onClick = { onDone(state.capturedUris) },
                            modifier = Modifier.heightIn(min = 52.dp),
                        ) {
                            Icon(Icons.Outlined.Check, null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.camera_done))
                        }
                    }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.camera_permission_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.camera_permission_message),
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 20.dp)) {
                    Text(stringResource(R.string.camera_permission_action))
                }
            }
        }
    }
}

@Composable
private fun CapturedPageIndicator(lastUri: String?, count: Int) {
    val thumbnail by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, lastUri) {
        value = withContext(Dispatchers.IO) {
            val path = lastUri?.let(Uri::parse)?.path ?: return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val longSide = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            val sample = Integer.highestOneBit((longSide / 160).coerceAtLeast(1))
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
        }
    }
    Surface(
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            thumbnail?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            if (count > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(3.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(count.toString(), Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun LiveDocumentOverlay(quad: DocumentQuad?, confidence: DocumentDetectionConfidence, modifier: Modifier = Modifier) {
    val color = when (confidence) {
        DocumentDetectionConfidence.HIGH -> Color(0xFFFF6A00)
        DocumentDetectionConfidence.MEDIUM -> Color(0xFFFFB15A)
        DocumentDetectionConfidence.LOW -> Color.White
        DocumentDetectionConfidence.NONE -> Color.Transparent
    }
    Canvas(modifier) {
        quad ?: return@Canvas
        val points = quad.asList().map { Offset(it.x * size.width, it.y * size.height) }
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(path, color.copy(alpha = 0.14f))
        drawPath(path, color, style = Stroke(width = 5f))
        points.forEach { drawCircle(color, radius = 8f, center = it) }
    }
}

@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onFrame: (IntArray, Int, Int) -> Unit,
    onCameraReady: (Camera) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(lifecycleOwner, imageCapture) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var boundProvider: ProcessCameraProvider? = null
        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(960, 720))
                    .build()
                    .also { useCase ->
                        useCase.setAnalyzer(analysisExecutor) { image ->
                            try {
                                image.toRotatedLumaFrame()?.let { frame -> onFrame(frame.luma, frame.width, frame.height) }
                            } finally {
                                image.close()
                            }
                        }
                    }
                cameraProvider.unbindAll()
                val boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                    imageCapture,
                )
                previewView.setOnTouchListener { _, event ->
                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                        val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                        boundCamera.cameraControl.startFocusAndMetering(
                            FocusMeteringAction.Builder(point).setAutoCancelDuration(3, TimeUnit.SECONDS).build(),
                        )
                    }
                    true
                }
                boundProvider = cameraProvider
                onCameraReady(boundCamera)
            } catch (_: Exception) {
                onError()
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            boundProvider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }
    AndroidView(modifier = modifier.background(Color.Black), factory = { previewView })
}

private data class LumaFrame(val luma: IntArray, val width: Int, val height: Int)

private fun ImageProxy.toRotatedLumaFrame(): LumaFrame? {
    val plane = planes.firstOrNull() ?: return null
    val buffer = plane.buffer
    val dense = IntArray(width * height)
    for (y in 0 until height) {
        val row = y * plane.rowStride
        for (x in 0 until width) dense[y * width + x] = buffer.get(row + x * plane.pixelStride).toInt() and 0xFF
    }
    return when (imageInfo.rotationDegrees) {
        90 -> LumaFrame(IntArray(dense.size) { index ->
            val targetX = index % height
            val targetY = index / height
            dense[(height - 1 - targetX) * width + targetY]
        }, height, width)
        180 -> LumaFrame(IntArray(dense.size) { dense[dense.lastIndex - it] }, width, height)
        270 -> LumaFrame(IntArray(dense.size) { index ->
            val targetX = index % height
            val targetY = index / height
            dense[targetX * width + (width - 1 - targetY)]
        }, height, width)
        else -> LumaFrame(dense, width, height)
    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (String) -> Unit,
    onFailure: () -> Unit,
) {
    val outputFile = File(context.cacheDir, "capture-${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) =
                onCaptured(Uri.fromFile(outputFile).toString())

            override fun onError(exception: ImageCaptureException) = onFailure()
        },
    )
}
