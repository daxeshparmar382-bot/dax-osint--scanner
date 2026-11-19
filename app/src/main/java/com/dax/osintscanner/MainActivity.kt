package com.dax.osintscanner

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.net.URI
import java.util.Locale

private val Navy = Color(0xFF090D1F)
private val Card = Color(0xFF12182E)
private val Purple = Color(0xFF8B5CF6)
private val Blue = Color(0xFF3B82F6)
private val Cyan = Color(0xFF22D3EE)
private val Muted = Color(0xFF94A3B8)
private val Green = Color(0xFF34D399)

@Composable
fun DaxApp(context: Context) {
    val history = remember { HistoryStore(context) }
    var selected by remember { mutableStateOf("dashboard") }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Purple,
        secondary = Cyan,
        background = Navy,
        surface = Card,
        onBackground = Color.White,
        onSurface = Color.White
    )) {
        Scaffold(
            containerColor = Navy,
            bottomBar = { BottomNav(selected) { selected = it } }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (selected) {
                    "dashboard" -> DashboardScreen(onNavigate = { selected = it })
                    "username" -> UsernameScreen(history)
                    "website" -> WebsiteScreen(history)
                    "privacy" -> PrivacyScreen()
                    "history" -> HistoryScreen(history)
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("DAX OSINT", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("SCANNER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Cyan, letterSpacing = 4.sp)
        Spacer(Modifier.height(24.dp))

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(24.dp)) {
            Box(Modifier.background(Brush.linearGradient(listOf(Blue.copy(.42f), Purple.copy(.35f)))).padding(20.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = Green, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PUBLIC INFORMATION TOOL", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Scan public identity footprints", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Ethical, consent-aware lookups", color = Muted, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Tools", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolCard(Modifier.weight(1f), Icons.Default.PersonSearch, "Username", "Public profile links") { onNavigate("username") }
            ToolCard(Modifier.weight(1f), Icons.Default.Language, "Website", "Domain & DNS lookup") { onNavigate("website") }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolCard(Modifier.weight(1f), Icons.Default.Lock, "Privacy", "Checklist") { onNavigate("privacy") }
            ToolCard(Modifier.weight(1f), Icons.Default.History, "History", "Past scans") { onNavigate("history") }
        }
        Spacer(Modifier.height(20.dp))

        PrivacyNotice()
    }
}

@Composable
private fun ToolCard(modifier: Modifier, icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier.height(138.dp).clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Purple.copy(.16f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Purple)
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun UsernameScreen(history: HistoryStore) {
    var username by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ProfileLink>>(emptyList()) }
    val scope = rememberCoroutineScope()

    ScanScreen("Username Search", Icons.Default.PersonSearch) {
        SearchField(username, "e.g. open_source_dev", KeyboardType.Text) { username = it }
        Spacer(Modifier.height(12.dp))
        GradientButton("Generate Public Links", enabled = username.trim().isNotEmpty()) {
            val clean = username.trim().removePrefix("@").replace(" ", "")
            results = publicProfileLinks(clean)
            history.add(ScanRecord("Username", clean, "${results.size} public links generated"))
        }
        Spacer(Modifier.height(18.dp))
        if (results.isNotEmpty()) {
            Text("Public destinations", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            results.forEach { link -> ProfileRow(link) }
        }
    }
}

@Composable
private fun WebsiteScreen(history: HistoryStore) {
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<WebsiteResult?>(null) }
    val scope = rememberCoroutineScope()

    ScanScreen("Website Lookup", Icons.Default.Language) {
        SearchField(input, "example.com", KeyboardType.Uri) { input = it }
        Spacer(Modifier.height(12.dp))
        GradientButton("Run Lookup", enabled = input.trim().isNotEmpty() && !loading) {
            val raw = input.trim()
            val domain = raw.substringAfter("//").substringBefore("/").substringBefore(":").removePrefix("www.")
            scope.launch {
                loading = true
                result = lookupWebsite(domain)
                loading = false
                history.add(ScanRecord("Website", domain, result?.summary ?: "Lookup complete"))
            }
        }
        Spacer(Modifier.height(18.dp))
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Cyan)
        result?.let { data ->
            Text("Lookup result", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            ResultCard("Domain", data.domain)
            ResultCard("Connection", data.status)
            ResultCard("Public IP addresses", data.addresses.ifEmpty { "Not resolved" })
            Text(data.summary, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

suspend fun lookupWebsite(domain: String): WebsiteResult = withContext(Dispatchers.IO) {
    val clean = domain.lowercase(Locale.US).trim().trimEnd('.').take(253)
    val addresses = try { InetAddress.getAllByName(clean).map { it.hostAddress }.distinct().take(8) } catch (_: Exception) { emptyList() }
    if (addresses.isEmpty()) return@withContext WebsiteResult(clean, "Could not resolve", "", "DNS resolution failed or network access is unavailable.")

    val client = OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build()
    val request = Request.Builder().url("https://$clean/").header("User-Agent", "DAX-OSINT-SCANNER/1.0").build()
    val status = try {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) "HTTPS reachable (${response.code})" else "HTTPS response (${response.code})"
        }
    } catch (_: Exception) {
        "DNS resolved; HTTPS probe unavailable"
    }
    WebsiteResult(clean, status, addresses.joinToString("\n"), "Public DNS and reachability data only. No vulnerability or hidden-data claims.")
}

@Composable
private fun PrivacyScreen() {
    val items = listOf(
        "Use unique passwords for every account",
        "Enable two-factor authentication",
        "Remove exposed phone numbers and addresses",
        "Review app permissions regularly",
        "Audit public social media privacy settings",
        "Do not publish live location or travel plans",
        "Delete unused accounts and stored API keys",
        "Use a password manager"
    )
    val checked = remember { mutableStateListOf(*Array(items.size) { false }) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(8.dp)); HeaderTitle("Privacy Checklist", Icons.Default.Lock)
        Spacer(Modifier.height(16.dp))
        Text("${checked.count { it }} of ${items.size} complete", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { checked.count { it }.toFloat() / items.size }, Modifier.fillMaxWidth(), color = Purple)
        Spacer(Modifier.height(16.dp))
        items.forEachIndexed { index, label ->
            Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillWidth().padding(vertical = 5.dp)) {
                Row(Modifier.fillMaxWidth().clickable { checked[index] = !checked[index] }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked[index], { checked[index] = it }, colors = CheckboxDefaults.colors(checkedColor = Purple))
                    Spacer(Modifier.width(8.dp)); Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(history: HistoryStore) {
    var records by remember { mutableStateOf(history.read()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(8.dp)); HeaderTitle("Scan History", Icons.Default.History)
        Spacer(Modifier.height(16.dp))
        if (records.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, tint = Muted, modifier = Modifier.size(52.dp)); Spacer(Modifier.height(12.dp)); Text("No scans yet", color = Muted)
                }
            }
        } else {
            records.forEach { record ->
                Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(record.type, color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(record.time.take(16), color = Muted, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(6.dp)); Text(record.query, fontWeight = FontWeight.Bold)
                        Text(record.result, color = Muted, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { history.clear(); records = emptyList() }, modifier = Modifier.fillMaxWidth()) { Text("Clear History") }
        }
    }
}

@Composable
private fun ScanScreen(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Spacer(Modifier.height(8.dp)); HeaderTitle(title, icon); Spacer(Modifier.height(18.dp)); content()
    }
}

@Composable
private fun HeaderTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Purple); Spacer(Modifier.width(10.dp)); Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SearchField(value: String, placeholder: String, type: KeyboardType, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text(placeholder, color = Muted) }, singleLine = true,
        shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = type, imeAction = ImeAction.Done), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF2A334F), unfocusedContainerColor = Card, focusedContainerColor = Card))
}

@Composable
private fun GradientButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple, disabledContainerColor = Purple.copy(.35f))) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ProfileRow(link: ProfileLink) {
    val uriHandler = LocalUriHandler.current
    Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { uriHandler.openUri(link.url) }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(link.icon, null, tint = Cyan, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
                Text(link.name, fontWeight = FontWeight.Bold); Text(link.url, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }; Icon(Icons.Default.OpenInNew, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ResultCard(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) { Text(label, color = Muted, fontSize = 11.sp); Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp)) }
    }
}

@Composable
private fun PrivacyNotice() {
    Card(colors = CardDefaults.cardColors(containerColor = Purple.copy(.10f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Purple.copy(.28f), RoundedCornerShape(16.dp))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, null, tint = Purple, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Text("Use only for lawful purposes and only for information you are authorized to view.", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun BottomNav(selected: String, onSelected: (String) -> Unit) {
    NavigationBar(containerColor = Color(0xFF0D1328), tonalElevation = 0.dp) {
        val items = listOf(
            Triple("dashboard", "Home", Icons.Default.Dashboard),
            Triple("username", "Username", Icons.Default.PersonSearch),
            Triple("website", "Website", Icons.Default.Language),
            Triple("privacy", "Privacy", Icons.Default.Lock),
            Triple("history", "History", Icons.Default.History)
        )
        items.forEach { (key, label, icon) -> NavigationBarItem(selected = selected == key, onClick = { onSelected(key) }, icon = { Icon(icon, null) }, label = { Text(label, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Purple, selectedTextColor = Purple, indicatorColor = Purple.copy(.14f), unselectedIconColor = Muted, unselectedTextColor = Muted)) }
    }
}

private fun publicProfileLinks(username: String): List<ProfileLink> {
    if (username.isBlank()) return emptyList()
    val safe = username.filter { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }.take(100)
    if (safe.isEmpty()) return emptyList()
    val encoded = java.net.URLEncoder.encode(safe, "UTF-8")
    return listOf(
        ProfileLink("GitHub", "https://github.com/$encoded", Icons.Default.Code),
        ProfileLink("GitLab", "https://gitlab.com/$encoded", Icons.Default.Terminal),
        ProfileLink("Reddit", "https://www.reddit.com/user/$encoded", Icons.Default.Forum),
        ProfileLink("TikTok", "https://www.tiktok.com/@$encoded", Icons.Default.PlayCircle),
        ProfileLink("Instagram", "https://www.instagram.com/$encoded", Icons.Default.CameraAlt),
        ProfileLink("YouTube", "https://www.youtube.com/@$encoded", Icons.Default.OndemandVideo)
    )
}

data class ProfileLink(val name: String, val url: String, val icon: ImageVector)
data class WebsiteResult(val domain: String, val status: String, val addresses: String, val summary: String)
