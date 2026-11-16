package page.kakkamvellytemple.app.presentation.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
actual fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    cacheEnabled: Boolean,
) {
    val context = LocalContext.current
    // Coil3 does not honor HTTP Cache-Control by default. Uploaded gateway
    // images must not bypass a fresh retirement/delivery check via disk/memory.
    val model = if (cacheEnabled) url else remember(url, context) {
        ImageRequest.Builder(context).data(url)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}
