package page.kakkamvellytemple.app.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import page.kakkamvellytemple.app.data.remote.ContactDto
import page.kakkamvellytemple.app.data.remote.DarshanDto
import page.kakkamvellytemple.app.data.remote.FestivalDto
import page.kakkamvellytemple.app.data.remote.GalleryDto
import page.kakkamvellytemple.app.data.remote.PoojaDto
import page.kakkamvellytemple.app.data.remote.TodayEventDto
import page.kakkamvellytemple.app.data.remote.VazhipadDto
import page.kakkamvellytemple.app.presentation.viewmodel.AdminViewModel

private fun minToHm(m: Int) = "%d:%02d".format(m / 60, m % 60)
private fun fmt12(h: Int, m: Int): String {
    val ampm = if (h < 12) "AM" else "PM"
    val hh = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
    return "%d:%02d %s".format(hh, m, ampm)
}
private fun hmToMin(s: String): Int? {
    val p = s.trim().split(":", limit = 2)
    val h = p.getOrNull(0)?.toIntOrNull() ?: return null
    val m = p.getOrNull(1)?.toIntOrNull() ?: 0
    return if (h in 0..23 && m in 0..59) h * 60 + m else null
}

/** Demo admin console: edits apply locally only until connected backend + auth
 *  scope are approved. No sign-in, no network, no real sends. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(onBack: () -> Unit, isEn: Boolean = false, vm: AdminViewModel = remember { AdminViewModel() }) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEn) "Admin (Demo)" else "അഡ്മിൻ (ഡെമോ)") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    if (isEn) "Demo mode: changes stay on this phone. Sign-in and publishing turn on after the backend is approved."
                    else "ഡെമോ മോഡ്: മാറ്റങ്ങൾ ഈ ഫോണിൽ മാത്രം. ബാക്കെൻഡ് അംഗീകരിച്ച ശേഷം മാത്രം പ്രസിദ്ധീകരണം.",
                    Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            if (vm.authState != null) {
                AdminSignInCard(vm = vm, isEn = isEn)
            }
            vm.status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }

            // ── Today's events editor ──
            Text(if (isEn) "Today's Events" else "ഇന്നത്തെ പരിപാടികൾ",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            vm.events.forEachIndexed { i, e ->
                ListItem(
                    headlineContent = { Text("${e.icon} ${if (isEn) e.titleEn else e.titleMl}") },
                    supportingContent = { Text(if (isEn) e.timeEn else e.timeMl) },
                    trailingContent = {
                        IconButton(onClick = { vm.removeEvent(i) }) { Icon(Icons.Default.Delete, "Remove") }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            var icon by remember { mutableStateOf("🙏") }
            var titleMl by remember { mutableStateOf("") }
            var titleEn by remember { mutableStateOf("") }
            var time by remember { mutableStateOf("") }
            OutlinedTextField(icon, { icon = it }, label = { Text("Icon (emoji)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(titleMl, { titleMl = it }, label = { Text("Title (Malayalam)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(titleEn, { titleEn = it }, label = { Text("Title (English)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(time, { time = it }, label = { Text("Time (e.g. 6:00 AM)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedButton(
                onClick = {
                    if (titleMl.isNotBlank() && time.isNotBlank()) {
                        vm.addEvent(TodayEventDto(icon, titleMl, titleEn.ifBlank { titleMl }, time, time))
                        titleMl = ""; titleEn = ""; time = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isEn) "Add event" else "പരിപാടി ചേർക്കുക") }

            HorizontalDivider()

            // ── Darshan hours editor ──
            Text(if (isEn) "Darshan Hours (HH:MM, 24h)" else "ദർശന സമയം (HH:MM)",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val d = vm.darshan
            var mo by remember(d) { mutableStateOf(minToHm(d.morningOpenMin)) }
            var mc by remember(d) { mutableStateOf(minToHm(d.morningCloseMin)) }
            var eo by remember(d) { mutableStateOf(minToHm(d.eveningOpenMin)) }
            var ec by remember(d) { mutableStateOf(minToHm(d.eveningCloseMin)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(mo, { mo = it }, label = { Text("Open AM") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(mc, { mc = it }, label = { Text("Close AM") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(eo, { eo = it }, label = { Text("Open PM") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(ec, { ec = it }, label = { Text("Close PM") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    val parsed = listOf(mo, mc, eo, ec).map { hmToMin(it) }
                    if (parsed.all { it != null }) {
                        vm.updateDarshan(DarshanDto(parsed[0]!!, parsed[1]!!, parsed[2]!!, parsed[3]!!))
                        vm.publish()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isEn) "Apply content changes (local)" else "മാറ്റങ്ങൾ പ്രയോഗിക്കുക (ലോക്കൽ)") }

            HorizontalDivider()

            // ── Pooja timings editor ──
            Text(if (isEn) "Pooja Timings" else "പൂജാ സമയം",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            var editPooja by remember { mutableStateOf<Triple<Boolean, Int, PoojaDto>?>(null) }
            listOf(true to vm.morningPoojas, false to vm.eveningPoojas).forEach { (morning, list) ->
                Text(if (morning) (if (isEn) "Morning" else "രാവിലെ") else (if (isEn) "Evening" else "വൈകീട്ട്"),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                list.forEachIndexed { i, p ->
                    ListItem(
                        modifier = Modifier.clickable { editPooja = Triple(morning, i, p) },
                        headlineContent = { Text(if (isEn) p.nameEn else p.nameMl) },
                        trailingContent = { Text(p.time, fontWeight = FontWeight.Bold) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
            editPooja?.let { (morning, idx, pooja) ->
                var hm by remember { mutableStateOf(minToHm(pooja.hour * 60 + pooja.minute)) }
                AlertDialog(
                    onDismissRequest = { editPooja = null },
                    title = { Text(if (isEn) pooja.nameEn else pooja.nameMl) },
                    text = {
                        OutlinedTextField(hm, { hm = it },
                            label = { Text("Time (HH:MM, 24h)") }, singleLine = true)
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            hmToMin(hm)?.let { mins ->
                                vm.updatePooja(morning, idx, pooja.copy(
                                    hour = mins / 60, minute = mins % 60,
                                    time = fmt12(mins / 60, mins % 60)))
                            }
                            editPooja = null
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { editPooja = null }) { Text("Cancel") } }
                )
            }

            HorizontalDivider()

            // ── Vazhipad rates editor ──
            Text(if (isEn) "Vazhipad Rates" else "വഴിപാട് നിരക്ക്",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            var editVaz by remember { mutableStateOf<Pair<Int, VazhipadDto>?>(null) }
            vm.vazhipad.forEachIndexed { i, v ->
                ListItem(
                    modifier = Modifier.clickable { editVaz = i to v },
                    headlineContent = { Text(if (isEn) v.nameEn else v.nameMl, style = MaterialTheme.typography.bodyMedium) },
                    trailingContent = { Text("₹${v.amount}", fontWeight = FontWeight.Bold) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            editVaz?.let { (idx, vaz) ->
                var amt by remember { mutableStateOf(vaz.amount.toString()) }
                AlertDialog(
                    onDismissRequest = { editVaz = null },
                    title = { Text(if (isEn) vaz.nameEn else vaz.nameMl) },
                    text = {
                        OutlinedTextField(amt, { amt = it.filter { c -> c.isDigit() } },
                            label = { Text("Amount (₹)") }, singleLine = true)
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            amt.toIntOrNull()?.let { vm.updateVazhipad(idx, vaz.copy(amount = it)) }
                            editVaz = null
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { editVaz = null }) { Text("Cancel") } }
                )
            }

            HorizontalDivider()

            // ── Contacts editor ──
            Text(if (isEn) "Contacts" else "ബന്ധപ്പെടാനുള്ളവർ",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            var editContact by remember { mutableStateOf<Pair<Int, ContactDto>?>(null) }
            vm.contacts.forEachIndexed { i, c ->
                ListItem(
                    modifier = Modifier.clickable { editContact = i to c },
                    headlineContent = { Text(if (isEn) c.roleEn else c.roleMl, style = MaterialTheme.typography.bodyMedium) },
                    trailingContent = { Text(c.phone, style = MaterialTheme.typography.labelMedium) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            editContact?.let { (idx, con) ->
                var phone by remember { mutableStateOf(con.phone) }
                var wa by remember { mutableStateOf(con.whatsapp ?: "") }
                AlertDialog(
                    onDismissRequest = { editContact = null },
                    title = { Text(if (isEn) con.roleEn else con.roleMl) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(phone, { phone = it }, label = { Text("Phone (+91...)") }, singleLine = true)
                            OutlinedTextField(wa, { wa = it }, label = { Text("WhatsApp (optional)") }, singleLine = true)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.updateContact(idx, con.copy(phone = phone.trim(),
                                whatsapp = wa.trim().ifBlank { null }))
                            editContact = null
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { editContact = null }) { Text("Cancel") } }
                )
            }

            HorizontalDivider()

            // ── Festival dates editor ──
            Text(if (isEn) "Festival Dates" else "ആഘോഷ തീയതികൾ",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            var editFest by remember { mutableStateOf<Pair<Int, FestivalDto>?>(null) }
            vm.festivals.forEachIndexed { i, f ->
                val dateStr = remember(f.dateUtcMs) {
                    val d = Instant.fromEpochMilliseconds(f.dateUtcMs).toLocalDateTime(TimeZone.UTC).date
                    "%04d-%02d-%02d".format(d.year, d.monthNumber, d.dayOfMonth)
                }
                ListItem(
                    modifier = Modifier.clickable { editFest = i to f },
                    headlineContent = { Text("${f.icon} ${if (isEn) f.nameEn else f.nameMl}", style = MaterialTheme.typography.bodyMedium) },
                    trailingContent = { Text(dateStr, style = MaterialTheme.typography.labelMedium) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            editFest?.let { (idx, fest) ->
                var dateTxt by remember {
                    mutableStateOf(run {
                        val d = Instant.fromEpochMilliseconds(fest.dateUtcMs).toLocalDateTime(TimeZone.UTC).date
                        "%04d-%02d-%02d".format(d.year, d.monthNumber, d.dayOfMonth)
                    })
                }
                var err by remember { mutableStateOf(false) }
                AlertDialog(
                    onDismissRequest = { editFest = null },
                    title = { Text(if (isEn) fest.nameEn else fest.nameMl) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(dateTxt, { dateTxt = it; err = false },
                                label = { Text("Date (YYYY-MM-DD)") }, singleLine = true,
                                isError = err)
                            if (err) Text("Invalid date", color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            try {
                                val d = LocalDate.parse(dateTxt.trim())
                                vm.updateFestival(idx, fest.copy(
                                    dateUtcMs = d.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()))
                                editFest = null
                            } catch (_: Exception) { err = true }
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { editFest = null }) { Text("Cancel") } }
                )
            }

            HorizontalDivider()

            // ── Gallery captions editor (image files stay on the website host) ──
            Text(if (isEn) "Gallery Captions" else "ഗ്യാലറി അടിക്കുറിപ്പുകൾ",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            var editCap by remember { mutableStateOf<Pair<Int, GalleryDto>?>(null) }
            vm.gallery.forEachIndexed { i, g ->
                ListItem(
                    modifier = Modifier.clickable { editCap = i to g },
                    headlineContent = { Text(if (isEn) g.captionEn else g.captionMl, style = MaterialTheme.typography.bodyMedium) },
                    supportingContent = { Text(g.resName, style = MaterialTheme.typography.labelSmall) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            editCap?.let { (idx, gal) ->
                var capMl by remember { mutableStateOf(gal.captionMl) }
                var capEn by remember { mutableStateOf(gal.captionEn) }
                AlertDialog(
                    onDismissRequest = { editCap = null },
                    title = { Text(gal.resName) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(capMl, { capMl = it }, label = { Text("Caption (Malayalam)") }, singleLine = true)
                            OutlinedTextField(capEn, { capEn = it }, label = { Text("Caption (English)") }, singleLine = true)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            vm.updateGalleryCaption(idx, gal.copy(captionMl = capMl, captionEn = capEn))
                            editCap = null
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { editCap = null }) { Text("Cancel") } }
                )
            }

            HorizontalDivider()

            // ── Notification composer (recorded only) ──
            Text(if (isEn) "Compose Notification" else "അറിയിപ്പ് തയ്യാറാക്കുക",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            var nTitle by remember { mutableStateOf("") }
            var nBody by remember { mutableStateOf("") }
            var nTopic by remember { mutableStateOf("announcements") }
            OutlinedTextField(nTitle, { nTitle = it }, label = { Text(if (isEn) "Title" else "തലക്കെട്ട്") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(nBody, { nBody = it }, label = { Text(if (isEn) "Message" else "സന്ദേശം") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (isEn) "Topic:" else "വിഭാഗം:", style = MaterialTheme.typography.bodySmall)
                listOf("announcements", "festivals", "annadhanam").forEach { t ->
                    FilterChip(selected = nTopic == t, onClick = { nTopic = t }, label = { Text(t, style = MaterialTheme.typography.labelSmall) })
                }
            }
            OutlinedButton(
                onClick = { if (nTitle.isNotBlank()) vm.sendNotification(nTitle, nBody, nTopic) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isEn) "Send (demo - not sent)" else "അയക്കുക (ഡെമോ - അയക്കില്ല)") }

            Spacer(Modifier.height(24.dp))
        }
    }
}
