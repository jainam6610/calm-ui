package com.studentlauncher

import android.Manifest
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.studentlauncher.ui.LauncherRoot

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = vm.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        vm.endSession() // crash recovery: close any orphan session

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(pkgReceiver, filter)

        setContent {
            val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) vm.setPhotoWallpaper(uri)
            }
            val multiPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
                if (uris != null && uris.isNotEmpty()) vm.setShufflePhotos(uris)
            }
            val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (!granted) vm.toast = "Allow notifications so reminders can appear"
            }
            LauncherRoot(
                vm,
                pickPhoto = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                pickShufflePhotos = { multiPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                addWidget = { addWidget(it) },
                askNotifications = {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        vm.onActivityStart()
        vm.startSignals()
        vm.hostMgr.start()
    }

    override fun onResume() {
        super.onResume()
        // End any tracked session before cancelling reminders
        vm.endSession()
        FocusReminders.cancel(this)
        vm.refreshShuffleSlot()
    }

    override fun onStop() {
        vm.onActivityStop()
        vm.stopSignals()
        vm.hostMgr.stop()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.onHomePressed()
    }

    // ---- Android widget hosting: allocate id -> bind (ask the user if needed) -> configure -> commit ----
    private fun addWidget(p: AppWidgetProviderInfo) {
        val id = vm.hostMgr.host.allocateAppWidgetId()
        vm.pendingWidgetId = id
        if (vm.hostMgr.mgr.bindAppWidgetIdIfAllowed(id, p.provider)) {
            afterBind(id)
        } else {
            val i = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, p.provider)
            @Suppress("DEPRECATION")
            startActivityForResult(i, REQ_BIND)
        }
    }

    private fun afterBind(id: Int) {
        val info = vm.hostMgr.mgr.getAppWidgetInfo(id)
        if (info?.configure != null) {
            vm.hostMgr.host.startAppWidgetConfigureActivityForResult(this, id, 0, REQ_CONFIG, null)
        } else {
            vm.commitAndroidWidget(id)
        }
    }

    @Deprecated("Widget hosting still uses request codes")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_BIND -> if (resultCode == RESULT_OK) afterBind(vm.pendingWidgetId) else vm.cancelPendingWidget()
            REQ_CONFIG -> if (resultCode == RESULT_OK) vm.commitAndroidWidget(vm.pendingWidgetId) else vm.cancelPendingWidget()
        }
    }

    override fun onDestroy() {
        vm.onActivityStop()
        runCatching { unregisterReceiver(pkgReceiver) }
        super.onDestroy()
    }

    companion object {
        private const val REQ_BIND = 4001
        private const val REQ_CONFIG = 4002
    }
}
