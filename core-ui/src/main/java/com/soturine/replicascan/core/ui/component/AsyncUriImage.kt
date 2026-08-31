package com.soturine.replicascan.core.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.soturine.replicascan.core.common.image.CanonicalImageDecoder
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AsyncUriImage(
    imageUri: String?,
    fallbackImageUri: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    maxDimension: Int = 2048,
    rotationDegrees: Float = 0f,
    onBitmapLoaded: ((IntSize) -> Unit)? = null,
) {
    val context = LocalContext.current
    val loadState by produceState<ImageLoadState>(
        initialValue = ImageLoadState.Loading,
        key1 = imageUri,
        key2 = fallbackImageUri,
        key3 = maxDimension,
    ) {
        value = withContext(Dispatchers.IO) {
            val candidates = listOfNotNull(imageUri, fallbackImageUri)
                .filter(String::isNotBlank)
                .distinct()
            candidates.forEachIndexed { index, candidate ->
                val decoded = runCatching { decodeBitmapForPreview(context, candidate, maxDimension) }.getOrNull()
                if (decoded != null) {
                    return@withContext ImageLoadState.Success(decoded, usedFallback = index > 0)
                }
            }
            ImageLoadState.Error
        }
    }

    LaunchedEffect(loadState, onBitmapLoaded) {
        (loadState as? ImageLoadState.Success)?.bitmap?.let { loaded ->
            onBitmapLoaded?.invoke(IntSize(loaded.width, loaded.height))
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = loadState) {
            ImageLoadState.Loading -> CircularProgressIndicator()
            ImageLoadState.Error -> Icon(
                imageVector = Icons.Outlined.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is ImageLoadState.Success -> {
                Image(
                    bitmap = state.bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = rotationDegrees },
                    contentScale = contentScale,
                )
            }
        }
    }
}

private sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data object Error : ImageLoadState
    data class Success(
        val bitmap: Bitmap,
        val usedFallback: Boolean,
    ) : ImageLoadState
}

private fun decodeBitmapForPreview(
    context: Context,
    imageUri: String,
    maxDimension: Int,
): Bitmap? {
    if (imageUri.isBlank()) return null
    val uri = Uri.parse(imageUri)
    val cacheKey = "$imageUri|$maxDimension|${cacheStamp(uri)}"
    previewCache.get(cacheKey)?.let { return it }
    return CanonicalImageDecoder(context).decode(imageUri, maxDimension)?.bitmap?.also { decoded ->
        previewCache.put(cacheKey, decoded)
    }
}

private fun cacheStamp(uri: Uri): String {
    val file = when {
        uri.scheme.isNullOrBlank() -> File(uri.toString())
        uri.scheme == "file" -> File(uri.path.orEmpty())
        else -> null
    } ?: return "content"
    return "${file.lastModified()}-${file.length()}"
}

private val previewCache =
    object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(
            key: String,
            value: Bitmap,
        ): Int = value.byteCount / 1024
    }
