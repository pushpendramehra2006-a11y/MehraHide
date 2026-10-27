package com.mehra.hide

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

data class AppInfo(val label: String, val pkg: String, val icon: ImageBitmap)

fun loadApps(ctx: android.content.Context): List<AppInfo> {
    val pm = ctx.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0)
        .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm).toBitmap(96, 96).asImageBitmap()) }
        .filter { it.pkg != ctx.packageName }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { App(prefs) } }
    }
}

@Composable
fun App(prefs: Prefs) {
    val ctx = LocalContext.current
    val apps = remember { loadApps(ctx) }
    var on by remember { mutableStateOf(prefs.on) }
    var tab by remember { mutableIntStateOf(0) }
    var hiddenApps by remember { mutableStateOf(prefs.hiddenApps) }
    var folders by remember { mutableStateOf(prefs.hiddenFolders) }
    var pinDialog by remember { mutableStateOf(false) }

    fun turnOn() {
        prefs.on = true; on = true
        FileHider.setAll(ctx, folders, true)
        NotifBlocker.instance?.sweep()
    }
    fun turnOff() {
        prefs.on = false; on = false
        FileHider.setAll(ctx, folders, false)
    }
    fun onToggle() {
        if (on) { if (prefs.pin.isEmpty()) turnOff() else pinDialog = true } else turnOn()
    }

    Scaffold(bottomBar = {
        NavigationBar {
            listOf("Home", "Control", "Apps", "Files").forEachIndexed { i, t ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = {}, label = { Text(t) })
            }
        }
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> HomeGrid(apps.filter { !(on && it.pkg in hiddenApps) })
                1 -> ControlTab(prefs, on, ::onToggle)
                2 -> LazyColumn(Modifier.padding(12.dp)) {
                    items(apps) { a ->
                        Row(Modifier.fillMaxWidth().clickable {
                            hiddenApps = if (a.pkg in hiddenApps) hiddenApps - a.pkg else hiddenApps + a.pkg
                            prefs.hiddenApps = hiddenApps
                        }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(a.icon, null, Modifier.size(40.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(a.label, Modifier.weight(1f))
                            Checkbox(a.pkg in hiddenApps, null)
                        }
                    }
                }
                3 -> {
                    val list = remember { FileHider.candidates() }
                    val root = Environment.getExternalStorageDirectory().absolutePath
                    LazyColumn(Modifier.padding(12.dp)) {
                        item { Text("Photo/Video folders jo chhupane hain (All files access zaroori)", Modifier.padding(8.dp)) }
                        items(list) { path ->
                            Row(Modifier.fillMaxWidth().clickable {
                                val hide = path !in folders
                                folders = if (hide) folders + path else folders - path
                                prefs.hiddenFolders = folders
                                if (on) FileHider.set(ctx, path, hide)
                                if (!hide && on) FileHider.set(ctx, path, false)
                            }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(path.removePrefix(root), Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Checkbox(path in folders, null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (pinDialog) {
        var t by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pinDialog = false },
            title = { Text("PIN daalo") },
            text = {
                OutlinedTextField(t, { t = it }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
            },
            confirmButton = {
                TextButton({
                    if (t == prefs.pin) { turnOff(); pinDialog = false }
                    else Toast.makeText(ctx, "Galat PIN", Toast.LENGTH_SHORT).show()
                }) { Text("Unhide") }
            }
        )
    }
}

@Composable
fun HomeGrid(apps: List<AppInfo>) {
    val ctx = LocalContext.current
    LazyVerticalGrid(GridCells.Fixed(4), Modifier.padding(8.dp)) {
        items(apps) { a ->
            Column(Modifier.clickable {
                ctx.packageManager.getLaunchIntentForPackage(a.pkg)?.let { ctx.startActivity(it) }
            }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(a.icon, null, Modifier.size(52.dp))
                Text(a.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ControlTab(prefs: Prefs, on: Boolean, onToggle: () -> Unit) {
    val ctx = LocalContext.current
    var pin by remember { mutableStateOf(prefs.pin) }
    Column(Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Button(onToggle, Modifier.fillMaxWidth().height(90.dp)) {
            Text(if (on) "HIDE ON  (tap = unhide)" else "HIDE OFF  (tap = hide)", style = MaterialTheme.typography.titleLarge)
        }
        OutlinedTextField(pin, { pin = it; prefs.pin = it }, label = { Text("Unhide PIN (khali = bina PIN)") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        Text("Setup (ek baar):")
        OutlinedButton({ ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, Modifier.fillMaxWidth()) {
            Text("1. Notification access do")
        }
        OutlinedButton({
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${ctx.packageName}")))
        }, Modifier.fillMaxWidth()) { Text("2. All files access do") }
        OutlinedButton({ ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }, Modifier.fillMaxWidth()) {
            Text("3. Isko default launcher banao")
        }
    }
}
