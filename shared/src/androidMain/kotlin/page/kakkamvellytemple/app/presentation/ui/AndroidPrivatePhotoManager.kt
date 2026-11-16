package page.kakkamvellytemple.app.presentation.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import page.kakkamvellytemple.app.presentation.ui.screen.PrivatePhotoScreen
import page.kakkamvellytemple.app.presentation.viewmodel.PrivatePhotoViewModel

/** No production caller. Uses the system document picker with a transient read
 * grant, never requests library-wide permission or persists URI/bytes. */
@Composable
fun AndroidPrivatePhotoManager(vm: PrivatePhotoViewModel, isEn: Boolean = true) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var pickError by remember { mutableStateOf(false) }
    val preview=remember(vm.preview) {
        vm.preview?.let { bytes ->
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
            if(bounds.outWidth in 1..2560 && bounds.outHeight in 1..2560)
                BitmapFactory.decodeByteArray(bytes,0,bytes.size)?.asImageBitmap() else null
        }
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) scope.launch {
            pickError=false
            try {
                val mime=context.contentResolver.getType(uri) ?: error("Unknown type")
                val bytes=withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                        while(true) { val n=stream.read(buffer);if(n<0)break
                            if(out.size()+n>8*1024*1024)error("Too large")
                            out.write(buffer,0,n)
                        }
                        out.toByteArray()
                    } ?: error("Unavailable photo")
                }
                try { vm.upload(bytes,mime) } finally { bytes.fill(0) }
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { pickError=true }
        }
    }
    DisposableEffect(vm) { onDispose { vm.clear() } }
    androidx.compose.foundation.layout.Column {
        if(pickError) androidx.compose.material3.Text(if(isEn) "Could not read that photo. Choose a JPEG, PNG or WebP up to 8 MiB."
            else "ചിത്രം വായിക്കാനായില്ല. 8 MiB വരെയുള്ള JPEG, PNG അല്ലെങ്കിൽ WebP തിരഞ്ഞെടുക്കുക.")
        PrivatePhotoScreen(vm,{picker.launch(arrayOf("image/jpeg","image/png","image/webp"))},preview,isEn)
    }
}
