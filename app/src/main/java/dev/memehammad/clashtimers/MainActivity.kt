package dev.memehammad.clashtimers

import android.Manifest
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import dev.memehammad.clashtimers.ui.AppTheme
import dev.memehammad.clashtimers.ui.MainScreen
import dev.memehammad.clashtimers.ui.ScreenActions
import dev.memehammad.clashtimers.ui.ScreenExtras
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val village = mutableStateOf<Village?>(null)
    private val builderOverride = mutableIntStateOf(0)
    private val extras = mutableStateOf(ScreenExtras())
    private var update: String? = null
    private val storeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "villages" || key == "current") reload()
    }
    private val autoImport = mutableStateOf(AutoImport.State.SHIZUKU_MISSING)
    private val shizukuResult = rikka.shizuku.Shizuku.OnRequestPermissionResultListener { _, result ->
        if (result == PackageManager.PERMISSION_GRANTED) AutoImport.setEnabled(this, true)
        autoImport.value = AutoImport.state(this)
    }
    private val snackbar = SnackbarHostState()
    /** 0 = main screen, 1 = first-run setup, 2 = tutorial. */
    private val intro = mutableIntStateOf(0)
    private val notificationsOn = mutableStateOf(true)
    private val batteryOk = mutableStateOf(true)
    private val settingsOpen = mutableStateOf(false)

    private fun batteryUnrestricted(): Boolean =
        getSystemService(android.os.PowerManager::class.java)?.isIgnoringBatteryOptimizations(packageName) == true

    /** Asks Android to let the app run unrestricted, so phone makers' battery savers can't block alerts. */
    private fun askBattery() {
        try {
            @android.annotation.SuppressLint("BatteryLife")
            val i = android.content.Intent(
                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                android.net.Uri.parse("package:$packageName"),
            )
            startActivity(i)
        } catch (e: Exception) {
            startActivity(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { notificationsOn.value = notificationsAllowed() }

    private fun notificationsAllowed(): Boolean = Build.VERSION.SDK_INT < 33 ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reload()
        Store.register(this, storeListener)
        // First run (no village yet), or asked for explicitly.
        val firstRun = !Store.tutorialSeen(this) && Store.tags(this).isEmpty()
        intro.intValue = when {
            intent?.getBooleanExtra(EXTRA_SETUP, false) == true || firstRun -> 1
            intent?.getBooleanExtra(EXTRA_TUTORIAL, false) == true -> 2
            else -> 0
        }
        notificationsOn.value = notificationsAllowed()
        batteryOk.value = batteryUnrestricted()
        settingsOpen.value = intent?.getBooleanExtra("settings", false) == true
        handleShare(intent)

        setContent {
            AppTheme {
                if (intro.intValue == 1) {
                    dev.memehammad.clashtimers.ui.Setup(
                        notificationsOn = notificationsOn.value,
                        onNotifications = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        reminderMinutes = extras.value.reminderMinutes,
                        onReminder = actions.onReminder,
                        autoImport = autoImport.value,
                        onAutoImport = { onAutoImportClick() },
                        batteryOk = batteryOk.value,
                        onBattery = { askBattery() },
                        onDone = { intro.intValue = 2 },
                    )
                } else if (intro.intValue == 2) {
                    androidx.activity.compose.BackHandler { closeTutorial() }
                    dev.memehammad.clashtimers.ui.Tutorial { closeTutorial() }
                } else if (settingsOpen.value) {
                    androidx.activity.compose.BackHandler { settingsOpen.value = false }
                    dev.memehammad.clashtimers.ui.SettingsScreen(
                        extras = extras.value,
                        actions = actions,
                        autoImport = autoImport.value,
                        onAutoImport = { onAutoImportClick() },
                        onBack = { settingsOpen.value = false },
                    )
                } else MainScreen(
                    village = village.value,
                    builderOverride = builderOverride.intValue,
                    snackbar = snackbar,
                    onPaste = { importClipboard(manual = true) },
                    onSetBuilders = { count ->
                        village.value?.let { Store.setBuilderOverride(this, it.tag, count) }
                        Refresh.all(this)
                        reload()
                    },
                    autoImport = autoImport.value,
                    onAutoImport = { onAutoImportClick() },
                    extras = extras.value,
                    actions = actions,
                )
            }
        }

        // Setup asks on first run; returning users are asked here.
        if (intro.intValue != 1 && !notificationsAllowed()) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onResume() {
        super.onResume()
        autoImport.value = AutoImport.state(this)
        notificationsOn.value = notificationsAllowed()
        batteryOk.value = batteryUnrestricted()
        AutoImport.ensure(this)
        // Auto-import may have added data while we were in the background.
        reload()
        LiveStatus.update(this)
        checkUpdate(force = false)
    }

    private fun checkUpdate(force: Boolean) {
        lifecycleScope.launch {
            update = Updates.check(this@MainActivity, force)?.version
            reload()
        }
    }

    private fun reload() {
        val v = Store.load(this)
        village.value = v
        builderOverride.intValue = v?.let { Store.builderOverride(this, it.tag) } ?: 0
        extras.value = ScreenExtras(
            villages = Store.tags(this).map { it to Store.name(this, it) },
            currentTag = v?.tag,
            reminderMinutes = Store.reminderMinutes(this),
            mutedSections = Store.mutedSections(this),
            liveUpdate = Store.liveUpdate(this),
            liveChip = Store.liveChip(this),
            timeMode = Store.timeMode(this),
            progressStyle = Store.progressStyle(this),
            staleHours = Store.staleHours(this),
            updateCheck = Store.updateCheck(this),
            update = update,
            version = BuildConfig.VERSION_NAME,
            batteryOk = batteryUnrestricted(),
        )
        Format.configure(this)
    }

    private fun closeTutorial() {
        Store.setTutorialSeen(this)
        intro.intValue = 0
    }

    private val actions = ScreenActions(
        onTutorial = { settingsOpen.value = false; intro.intValue = 2 },
        onBattery = { askBattery() },
        onSettings = { settingsOpen.value = true },
        onSelectVillage = { tag ->
            Store.setCurrentTag(this, tag)
            Refresh.all(this)
            reload()
        },
        onRenameVillage = { tag, name ->
            Store.setName(this, tag, name)
            Refresh.all(this)
            reload()
        },
        onRemoveVillage = { tag ->
            Store.remove(this, tag)
            Refresh.all(this)
            reload()
        },
        onMuteSection = { key, mute ->
            val m = Store.mutedSections(this)
            Store.setMutedSections(this, if (mute) m + key else m - key)
            Scheduler.reschedule(this)
            reload()
        },
        onReminder = { minutes ->
            Store.setReminderMinutes(this, minutes)
            Scheduler.reschedule(this)
            reload()
        },
        onLiveUpdate = { on ->
            Store.setLiveUpdate(this, on)
            LiveStatus.update(this)
            reload()
        },
        onUpdateCheck = { on ->
            Store.setUpdateCheck(this, on)
            if (on) checkUpdate(force = true) else update = null
            reload()
        },
        onStaleHours = { h ->
            Store.setStaleHours(this, h)
            Format.configure(this)
            Refresh.all(this)
            reload()
        },
        onProgressStyle = { style ->
            Store.setProgressStyle(this, style)
            Format.configure(this)
            WidgetProvider.updateAll(this)
            reload()
        },
        onTimeMode = { mode ->
            Store.setTimeMode(this, mode)
            Format.configure(this)
            WidgetProvider.updateAll(this)
            reload()
        },
        onLiveChip = { mode ->
            Store.setLiveChip(this, mode)
            LiveStatus.update(this)
            reload()
        },
    )

    override fun onDestroy() {
        rikka.shizuku.Shizuku.removeRequestPermissionResultListener(shizukuResult)
        Store.unregister(this, storeListener)
        super.onDestroy()
    }

    private fun onAutoImportClick() {
        when (AutoImport.state(this)) {
            AutoImport.State.SHIZUKU_MISSING -> {
                val launch = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    ?: Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://shizuku.rikka.app/download/"))
                startActivity(launch)
            }
            AutoImport.State.NEEDS_PERMISSION -> {
                rikka.shizuku.Shizuku.addRequestPermissionResultListener(shizukuResult)
                rikka.shizuku.Shizuku.requestPermission(REQUEST_SHIZUKU)
            }
            AutoImport.State.OFF -> AutoImport.setEnabled(this, true)
            AutoImport.State.ON -> AutoImport.setEnabled(this, false)
        }
        autoImport.value = AutoImport.state(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShare(intent)
    }

    // Android only allows clipboard reads while the app has focus, so check here.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) importClipboard(manual = false)
    }

    private fun handleShare(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        intent.action = null
        report(Importer.import(this, text), manual = true)
    }

    private fun importClipboard(manual: Boolean) {
        val cm = getSystemService(ClipboardManager::class.java) ?: return
        val desc = cm.primaryClipDescription
        if (desc == null) {
            if (manual) report(Importer.Result.INVALID, true)
            return
        }
        // Read each new clipboard entry only once (avoids the "pasted" toast on every open).
        if (!manual) {
            if (desc.timestamp == Store.clipStamp(this)) return
            Store.setClipStamp(this, desc.timestamp)
        }
        if (!desc.hasMimeType("text/*")) {
            report(Importer.Result.INVALID, manual)
            return
        }
        val text = try {
            cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)
        } catch (e: Exception) {
            null
        }
        report(Importer.import(this, text), manual)
    }

    private fun report(result: Importer.Result, manual: Boolean) {
        if (result != Importer.Result.INVALID) reload()
        val message = when (result) {
            Importer.Result.IMPORTED -> getString(R.string.imported)
            Importer.Result.UNCHANGED -> if (manual) getString(R.string.up_to_date) else null
            Importer.Result.INVALID -> if (manual) getString(R.string.no_data) else null
        } ?: return
        lifecycleScope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    companion object {
        private const val REQUEST_SHIZUKU = 42
        const val EXTRA_TUTORIAL = "tutorial"
        const val EXTRA_SETUP = "setup"
    }
}
