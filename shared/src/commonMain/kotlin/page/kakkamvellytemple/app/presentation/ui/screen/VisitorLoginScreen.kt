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

/** Prepared component, intentionally not installed as a production navigation
 * gate yet. Caller supplies an isolated visitor VM; never the admin VM. */
@Composable
fun VisitorLoginScreen(vm: VisitorLoginViewModel, onSignedIn: () -> Unit) {
    LaunchedEffect(vm.signedIn) { if (vm.signedIn) onSignedIn() }
    LaunchedEffect(vm) { while (true) { vm.tick(); delay(1000) } }
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Visitor login", style = MaterialTheme.typography.headlineMedium)
        Text("Enter your WhatsApp number. Your login code will arrive in WhatsApp, not by SMS.")
        if (!vm.deliveryEnabled) {
            Text("WhatsApp login is being set up. No code can be sent yet.", color = MaterialTheme.colorScheme.error)
        }
        OutlinedTextField(
            value = vm.phone, onValueChange = vm::updatePhone,
            label = { Text("WhatsApp number with country code") },
            placeholder = { Text("+countrycode number (digits only)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = !vm.busy && !vm.awaitingCode,
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        if (vm.awaitingCode) {
            OutlinedTextField(
                value = vm.code, onValueChange = vm::updateCode,
                label = { Text("Six-digit code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                enabled = !vm.busy, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = vm::verifyCode, enabled = !vm.busy && vm.deliveryEnabled) { Text("Verify code") }
            TextButton(onClick = vm::changeNumber, enabled = !vm.busy) { Text("Change number") }
        }
        Button(onClick = vm::requestCode,
            enabled = !vm.busy && vm.deliveryEnabled && vm.secondsUntilResend == 0) {
            Text(if (vm.secondsUntilResend > 0) "Resend in ${vm.secondsUntilResend}s"
                else if (vm.awaitingCode) "Resend WhatsApp code" else "Send WhatsApp code")
        }
        if (vm.busy) CircularProgressIndicator()
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Requesting a code asks the temple's login service to send it to this number. This does not subscribe you to temple announcements.",
            style = MaterialTheme.typography.bodySmall)
    }
}
