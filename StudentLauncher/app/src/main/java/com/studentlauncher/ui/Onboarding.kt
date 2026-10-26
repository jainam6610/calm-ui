package com.studentlauncher.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.R
import com.studentlauncher.data.AppInfo

private const val STEP_APPS = 1
private const val STEP_LIMIT = 2
private const val STEP_PIN = 3
private const val STEP_DONE = 4

/**
 * First-run setup, shown once (LauncherViewModel.onboardingDone). Picking distracting apps here
 * uses setFlaggedDirect rather than the PIN-guarded toggleFlag - there is no PIN yet at this point,
 * and this screen is exactly where one can be created if the person wants one.
 */
@Composable
fun OnboardingFlow(vm: LauncherViewModel) {
    val c = LocalColors.current
    var step by remember { mutableIntStateOf(STEP_APPS) }

    Box(Modifier.fillMaxSize().background(if (c.dark) Color.Black else Color(0xFFF2F2F4))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in STEP_APPS..STEP_DONE) {
                    Box(
                        Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp))
                            .background(if (i <= step) c.accent else c.track)
                    )
                }
            }
            Spacer(Modifier.height(28.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally(spring(0.86f, 320f)) { it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally(spring(0.86f, 320f)) { -it / 4 } + fadeOut())
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "onboarding"
            ) { s ->
                when (s) {
                    STEP_APPS -> AppsStep(vm)
                    STEP_LIMIT -> LimitStep(vm)
                    STEP_PIN -> PinStep(vm)
                    else -> DoneStep()
                }
            }

            Spacer(Modifier.height(16.dp))
            if (step == STEP_DONE) {
                val ctx = LocalContext.current
                if (!isDefaultHome(ctx)) {
                    PillButton("Set as default home", { requestDefaultHome(ctx) }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step == STEP_DONE) {
                    PillButton("Get started", { vm.completeOnboarding() }, filled = true, modifier = Modifier.fillMaxWidth())
                } else {
                    if (step != STEP_APPS) PillButton("Skip", { step = STEP_DONE })
                    PillButton(
                        if (step == STEP_APPS) "Next (${vm.flagged.size} picked)" else "Next",
                        { step++ }, filled = true
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeading(title: String, sub: String) {
    val c = LocalColors.current
    Txt(title, 24f, c.fg, FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    Txt(sub, 14f, c.fg2, maxLines = 3)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun AppsStep(vm: LauncherViewModel) {
    val apps = vm.apps
    Column(Modifier.fillMaxSize()) {
        StepHeading("What tends to distract you?", "Pick the apps you want Mindful Mode to watch. You can change this any time.")
        LazyColumn(Modifier.fillMaxSize()) {
            items(apps, key = { it.pkg }) { app -> AppPickRow(app, vm) }
        }
    }
}

@Composable
private fun AppPickRow(app: AppInfo, vm: LauncherViewModel) {
    val c = LocalColors.current
    val on = app.pkg in vm.flagged
    Row(
        Modifier.fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                vm.setFlaggedDirect(app.pkg, !on)
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconView(app, vm, 34.dp)
        Spacer(Modifier.width(14.dp))
        Txt(app.label, 15f, c.fg, modifier = Modifier.weight(1f))
        Box(Modifier.size(22.dp).clip(CircleShape).background(if (on) c.accent else c.track))
    }
}

@Composable
private fun LimitStep(vm: LauncherViewModel) {
    val engine = vm.mindful
    Column(Modifier.fillMaxSize()) {
        StepHeading("Set a daily limit", "How many minutes a day feel reasonable for those apps? You can set limits per app later.")
        Segmented(
            listOf(0 to "Off", 15 to "15", 30 to "30", 45 to "45", 60 to "60", 90 to "90"),
            engine.settings.budgetMin,
            { v -> engine.update { it.copy(budgetMin = v) } }
        )
    }
}

@Composable
private fun PinStep(vm: LauncherViewModel) {
    val c = LocalColors.current
    Column(Modifier.fillMaxSize()) {
        StepHeading("Protect your settings", "Optional. With a PIN set, opening Focus settings - Study mode, limits, reminders - needs it too.")
        if (vm.pinSet) {
            Txt("PIN set.", 15f, c.accent, FontWeight.Medium)
        } else {
            PillButton("Set a PIN", { vm.requestCreatePin() }, filled = true)
        }
    }
}

@Composable
private fun DoneStep() {
    val c = LocalColors.current
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.Start) {
        Spacer(Modifier.weight(1f))
        Txt("You're all set.", 26f, c.fg, FontWeight.Medium, align = TextAlign.Start)
        Spacer(Modifier.height(10.dp))
        Txt(stringResource(R.string.onboard_gesture_apps), 14f, c.fg2)
        Spacer(Modifier.height(7.dp))
        Txt(stringResource(R.string.onboard_gesture_rail), 14f, c.fg2)
        Spacer(Modifier.height(7.dp))
        Txt(stringResource(R.string.onboard_gesture_widget), 14f, c.fg2)
        Spacer(Modifier.height(7.dp))
        Txt(stringResource(R.string.onboard_gesture_board), 14f, c.fg2)
        Spacer(Modifier.height(7.dp))
        Txt(stringResource(R.string.onboard_gesture_app_actions), 14f, c.fg2)
        Spacer(Modifier.weight(1f))
    }
}

private fun isDefaultHome(ctx: Context): Boolean =
    ctx.packageManager.resolveActivity(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        PackageManager.MATCH_DEFAULT_ONLY
    )?.activityInfo?.packageName == ctx.packageName

// ponytail: RoleManager home-role dialog on API 29+, default-apps settings below that
private fun requestDefaultHome(ctx: Context) {
    val role = if (android.os.Build.VERSION.SDK_INT >= 29) {
        runCatching {
            ctx.getSystemService(android.app.role.RoleManager::class.java)
                ?.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
        }.getOrNull()
    } else null
    runCatching { ctx.startActivity(role ?: Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
}
