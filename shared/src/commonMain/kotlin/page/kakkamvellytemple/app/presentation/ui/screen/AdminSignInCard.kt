package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import page.kakkamvellytemple.app.data.repository.AdminAuthState
import page.kakkamvellytemple.app.presentation.viewmodel.AdminViewModel

/** OTP sign-in card for the admin console. Driven entirely by the repository's
 *  auth state. With a disabled repository the send action fails closed with a
 *  "not enabled" message and no network call happens. */
@Composable
fun AdminSignInCard(vm: AdminViewModel, isEn: Boolean) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (isEn) "Admin sign-in" else "അഡ്മിൻ സൈൻ-ഇൻ", style = MaterialTheme.typography.titleSmall)
            when (val s = vm.authState) {
                is AdminAuthState.SignedIn -> {
                    Text(
                        if (isEn) "Signed in. Admin writes are checked by the server."
                        else "സൈൻ-ഇൻ ചെയ്തു. അഡ്മിൻ എഴുത്തുകൾ സെർവർ പരിശോധിക്കും.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(onClick = { vm.signOut() }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isEn) "Sign out" else "സൈൻ ഔട്ട്")
                    }
                }
                is AdminAuthState.AwaitingOtp -> {
                    Text(
                        if (isEn) "Code sent to ${s.destination}" else "കോഡ് അയച്ചു: ${s.destination}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    var code by remember { mutableStateOf("") }
                    OutlinedTextField(
                        code, { code = it },
                        label = { Text(if (isEn) "6-digit code" else "6 അക്ക കോഡ്") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { vm.verifyOtp(s.destination, code.trim()) },
                        enabled = code.trim().length == 6,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isEn) "Verify" else "പരിശോധിക്കുക") }
                }
                else -> {
                    var dest by remember { mutableStateOf("") }
                    OutlinedTextField(
                        dest, { dest = it },
                        label = { Text(if (isEn) "Phone or email" else "ഫോൺ അല്ലെങ്കിൽ ഇമെയിൽ") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    if (s is AdminAuthState.Failed) {
                        Text(s.message, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = { vm.requestOtp(dest.trim()) },
                        enabled = dest.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isEn) "Send code" else "കോഡ് അയയ്ക്കുക") }
                }
            }
        }
    }
}
