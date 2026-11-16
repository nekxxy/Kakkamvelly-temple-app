package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import page.kakkamvellytemple.app.presentation.viewmodel.PrivatePhotoViewModel

/** Private management component, not linked into production navigation. Actual
 * device pixels, Malayalam copy and accessibility remain release gates. */
@Composable
fun PrivatePhotoScreen(vm: PrivatePhotoViewModel, onChoosePhoto: () -> Unit,
    preview: ImageBitmap? = null, isEn: Boolean = true) {
    fun text(en: String, ml: String) = if (isEn) en else ml
    LaunchedEffect(vm) { vm.refresh() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(text("Private temple photos", "സ്വകാര്യ ക്ഷേത്ര ചിത്രങ്ങൾ"),style=MaterialTheme.typography.headlineSmall)
        Text(text("Originals and previews stay private. Uploading or reviewing does not publish them. See each photo's publication status below.",
            "അസൽ ചിത്രങ്ങളും പ്രിവ്യൂകളും സ്വകാര്യമാണ്. അപ്‌ലോഡ് ചെയ്യുന്നതോ പരിശോധിക്കുന്നതോ പ്രസിദ്ധീകരണമല്ല. ഓരോ ചിത്രത്തിന്റെയും പ്രസിദ്ധീകരണ നില താഴെ കാണാം."))
        Button(onClick=onChoosePhoto,enabled=!vm.busy) { Text(text("Choose a photo", "ചിത്രം തിരഞ്ഞെടുക്കുക")) }
        Text(text("JPEG, PNG or WebP, up to 8 MiB. Location metadata is removed during server processing.",
            "JPEG, PNG അല്ലെങ്കിൽ WebP, പരമാവധി 8 MiB. സെർവർ പ്രോസസ്സിംഗിൽ ലൊക്കേഷൻ വിവരങ്ങൾ നീക്കംചെയ്യും."))
        TextButton(onClick=vm::refresh,enabled=!vm.busy) { Text(text("Refresh private list", "സ്വകാര്യ പട്ടിക പുതുക്കുക")) }
        if(vm.busy) CircularProgressIndicator()
        vm.error?.let { Text(if(isEn) it else "ചിത്രത്തിന്റെ പ്രവർത്തനം സ്ഥിരീകരിക്കാനായില്ല. ഒന്നും പ്രസിദ്ധീകരിച്ചിട്ടില്ല.",color=MaterialTheme.colorScheme.error) }
        vm.notice?.let { Text(if(isEn) it else "സ്വകാര്യ ചിത്രത്തിന്റെ നില പുതുക്കി. ഒന്നും പ്രസിദ്ധീകരിച്ചിട്ടില്ല.") }
        if(preview!=null) {
            Image(preview,contentDescription=text("Private photo preview", "സ്വകാര്യ ചിത്രത്തിന്റെ പ്രിവ്യൂ"),modifier=Modifier.fillMaxWidth().heightIn(max=360.dp))
            TextButton(onClick=vm::closePreview) { Text(text("Close preview", "പ്രിവ്യൂ അടയ്ക്കുക")) }
        }
        if (!vm.deliveryEnabled) Text(text("Visitor photo delivery is disabled.", "സന്ദർശകർക്കുള്ള ചിത്ര വിതരണം പ്രവർത്തനരഹിതമാണ്."))
        vm.photos.forEach { photo ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(photo.id,style=MaterialTheme.typography.labelSmall)
                    Text(when(photo.state) {
                        "pending" -> text("Private original: waiting for processing", "സ്വകാര്യ ചിത്രം: പ്രോസസ്സിംഗ് കാത്തിരിക്കുന്നു")
                        "processed" -> text("Processed privately: review needed", "സ്വകാര്യമായി പ്രോസസ്സ് ചെയ്തു: പരിശോധന വേണം")
                        else -> text("Private review recorded", "സ്വകാര്യ പരിശോധന രേഖപ്പെടുത്തി")
                    })
                    Text(when(photo.publication_state) {
                        "queued" -> text("Publication requested, not yet published", "പ്രസിദ്ധീകരണ അഭ്യർത്ഥന നൽകി, ഇതുവരെ പ്രസിദ്ധീകരിച്ചിട്ടില്ല")
                        "canceled" -> text("Publication request canceled", "പ്രസിദ്ധീകരണ അഭ്യർത്ഥന റദ്ദാക്കി")
                        "published" -> if(vm.deliveryEnabled) text("Published. Visitor delivery is enabled.", "പ്രസിദ്ധീകരിച്ചു. സന്ദർശകർക്കുള്ള വിതരണം പ്രവർത്തനക്ഷമമാണ്.")
                            else text("Published, but visitor delivery is disabled.", "പ്രസിദ്ധീകരിച്ചു, എന്നാൽ സന്ദർശകർക്കുള്ള വിതരണം പ്രവർത്തനരഹിതമാണ്.")
                        "retired" -> text("Retired. Future visitor reads are disabled.", "പിൻവലിച്ചു. ഭാവിയിലെ സന്ദർശക വായനകൾ പ്രവർത്തനരഹിതമാണ്.")
                        else -> text("Not published", "പ്രസിദ്ധീകരിച്ചിട്ടില്ല")
                    })
                    if(photo.preview_path!=null) TextButton(onClick={vm.showPreview(photo.id)},enabled=!vm.busy) {
                        Text(text("Preview processed photo", "പ്രോസസ്സ് ചെയ്ത ചിത്രം കാണുക"))
                    }
                    if(photo.state=="processed") TextButton(onClick={vm.review(photo.id)},enabled=!vm.busy) {
                        Text(text("Record private review", "സ്വകാര്യ പരിശോധന രേഖപ്പെടുത്തുക"))
                    }
                }
            }
        }
    }
}
