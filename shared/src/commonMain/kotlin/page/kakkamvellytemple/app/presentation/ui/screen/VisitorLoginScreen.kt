package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import page.kakkamvellytemple.app.presentation.viewmodel.VisitorLoginViewModel

/** Login component. Actual pixels and Malayalam copy still need human/device
 * review before enabling delivery or making login mandatory in a release. */
@Composable
fun VisitorLoginScreen(vm: VisitorLoginViewModel, onSignedIn: () -> Unit, isEn: Boolean = true) {
    fun text(en: String, ml: String) = if (isEn) en else ml
    LaunchedEffect(vm.signedIn) { if (vm.signedIn) onSignedIn() }
    LaunchedEffect(vm) { while (true) { vm.tick(); delay(1000) } }
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text("Visitor login", "സന്ദർശക ലോഗിൻ"), style = MaterialTheme.typography.headlineMedium)
        Text(text("Enter your WhatsApp number. Your login code will arrive in WhatsApp, not by SMS.",
            "നിങ്ങളുടെ വാട്സ്ആപ്പ് നമ്പർ നൽകുക. ലോഗിൻ കോഡ് വാട്സ്ആപ്പിൽ ലഭിക്കും, എസ്.എം.എസ്. വഴി അല്ല."))
        if (!vm.deliveryEnabled) {
            Text(text("WhatsApp login is being set up. No code can be sent yet.",
                "വാട്സ്ആപ്പ് ലോഗിൻ സജ്ജമാക്കുകയാണ്. ഇപ്പോൾ കോഡ് അയയ്ക്കാനാകില്ല."), color = MaterialTheme.colorScheme.error)
        }
        OutlinedTextField(vm.phone, vm::updatePhone,
            label = { Text(text("WhatsApp number with country code", "രാജ്യകോഡ് സഹിതം വാട്സ്ആപ്പ് നമ്പർ")) },
            placeholder = { Text("+91…") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = !vm.busy && !vm.awaitingCode, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (vm.awaitingCode) {
            OutlinedTextField(vm.code, vm::updateCode,
                label = { Text(text("Six-digit code", "ആറക്ക കോഡ്")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                enabled = !vm.busy, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = vm::verifyCode, enabled = !vm.busy && vm.deliveryEnabled) {
                Text(text("Verify code", "കോഡ് പരിശോധിക്കുക"))
            }
            TextButton(onClick = vm::changeNumber, enabled = !vm.busy) { Text(text("Change number", "നമ്പർ മാറ്റുക")) }
        }
        Button(onClick = vm::requestCode,
            enabled = !vm.busy && vm.deliveryEnabled && vm.secondsUntilResend == 0) {
            Text(if (vm.secondsUntilResend > 0)
                text("Resend in ${vm.secondsUntilResend}s", "${vm.secondsUntilResend} സെക്കൻഡിന് ശേഷം വീണ്ടും അയയ്ക്കാം")
                else if (vm.awaitingCode) text("Resend WhatsApp code", "വാട്സ്ആപ്പ് കോഡ് വീണ്ടും അയയ്ക്കുക")
                else text("Send WhatsApp code", "വാട്സ്ആപ്പ് കോഡ് അയയ്ക്കുക"))
        }
        if (vm.busy) CircularProgressIndicator()
        vm.error?.let {
            Text(if (isEn) it else "ലോഗിൻ പൂർത്തിയാക്കാനായില്ല. നമ്പറും കോഡും പരിശോധിച്ച് വീണ്ടും ശ്രമിക്കുക.",
                color = MaterialTheme.colorScheme.error)
        }
        Text(text("Requesting a code asks the temple's login service to send it to this number. This does not subscribe you to temple announcements.",
            "കോഡ് ആവശ്യപ്പെടുമ്പോൾ ക്ഷേത്രത്തിന്റെ ലോഗിൻ സേവനം ഈ നമ്പറിലേക്ക് കോഡ് അയയ്ക്കും. ഇത് ക്ഷേത്ര അറിയിപ്പുകൾക്കുള്ള സബ്സ്ക്രിപ്ഷൻ അല്ല."),
            style = MaterialTheme.typography.bodySmall)
    }
}

/** Optional navigation gate, off unless the caller explicitly opts in after
 * setup and verification. While held, content browsing remains unchanged.
 * Rechecks expiry on a cadence without resending OTP or invoking the sender. */
@Composable
fun VisitorSessionGate(vm: VisitorLoginViewModel, requireLogin: Boolean = false,
    isEn: Boolean = true, content: @Composable () -> Unit) {
    if (!requireLogin) { content(); return }
    var checked by remember(vm) { mutableStateOf(false) }
    LaunchedEffect(vm) {
        vm.restoreSession()
        checked = true
        while (true) { delay(30_000); vm.restoreSession() }
    }
    when {
        !checked || (vm.restoring && !vm.signedIn) -> CircularProgressIndicator()
        vm.signedIn -> content()
        else -> Column {
            if (vm.restoreFailed) {
                Text(if (isEn) "Could not restore your session. Check your connection and retry."
                    else "ലോഗിൻ പുനഃസ്ഥാപിക്കാനായില്ല. ഇന്റർനെറ്റ് ബന്ധം പരിശോധിച്ച് വീണ്ടും ശ്രമിക്കുക.")
                TextButton(onClick = vm::restoreSession) { Text(if (isEn) "Retry" else "വീണ്ടും ശ്രമിക്കുക") }
            }
            VisitorLoginScreen(vm, {}, isEn)
        }
    }
}
