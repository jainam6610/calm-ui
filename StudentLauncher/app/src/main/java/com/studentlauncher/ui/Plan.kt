package com.studentlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.InternalDestination
import com.studentlauncher.data.PlanTask
import java.time.LocalDate

/** First-party study planner. All information stays in launcher SharedPreferences. */
@Composable
fun PlanApp(vm: LauncherViewModel) {
    val c = LocalColors.current
    var week by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var addClass by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PlanTask?>(null) }
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }

    BackHandler { vm.destination = InternalDestination.Home }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Txt("PLAN", 11f, c.fg2, FontWeight.Bold)
                Txt(if (week) "This week" else "Today", 30f, c.fg, FontWeight.SemiBold)
            }
            PillButton("Home", { vm.destination = InternalDestination.Home })
        }

        Spacer(Modifier.height(18.dp))
        Segmented(listOf(false to "Today", true to "Week"), week, onSelect = { week = it })
        Spacer(Modifier.height(16.dp))
        if (week) DayChooser(selectedDay) { selectedDay = it }

        val activeDate = if (week) selectedDay.toString() else LocalDate.now().toString()
        val tasks = vm.planTasks.filter { it.due.isBlank() || it.due == activeDate }

        AnimatedContent(
            tasks.isEmpty(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "plan-state"
        ) { empty ->
            if (empty) {
                EmptyPlan { adding = true }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Txt("AGENDA", 11f, c.fg2, FontWeight.Bold)
                    val events = vm.planEvents.filter { it.date == activeDate }
                    if (events.isEmpty()) AgendaLine("Next class", "No class scheduled")
                    else events.forEach {
                        AgendaLine(
                            it.title,
                            listOf(it.course, it.time).filter { x -> x.isNotBlank() }.joinToString(" · ")
                        )
                    }
                    Txt("TASKS", 11f, c.fg2, FontWeight.Bold, Modifier.padding(top = 10.dp))
                    tasks
                        .sortedWith(compareBy<PlanTask> { it.completed }.thenByDescending { it.priority })
                        .forEach { PlanRow(it, vm) { editing = it } }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row {
            PillButton("+ Task", { adding = true }, Modifier.weight(1f), filled = true)
            Spacer(Modifier.width(10.dp))
            PillButton("+ Class", { addClass = true }, Modifier.weight(1f))
        }
    }

    if (adding) PlanEditor(onDismiss = { adding = false }) { title, due, priority, course ->
        if (vm.addPlanTask(title, due, priority, course)) adding = false
    }
    editing?.let { task ->
        PlanEditor(
            task,
            onDismiss = { editing = null },
            onDelete = { vm.removePlanTask(task.id); editing = null }
        ) { title, due, priority, course ->
            if (vm.updatePlanTask(task.copy(title = title, due = due, priority = priority, className = course))) editing = null
        }
    }
    if (addClass) ClassEditor(onDismiss = { addClass = false }) { title, date, time, course ->
        if (vm.addPlanEvent(title, date, time, course)) addClass = false
    }
}

@Composable
private fun AgendaLine(title: String, sub: String) {
    val c = LocalColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.tile)
            .padding(16.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.accent)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Txt(title, 15f, c.fg, FontWeight.Medium)
            Txt(sub, 12f, c.fg2, maxLines = 2)
        }
    }
}

@Composable
private fun EmptyPlan(onAdd: () -> Unit) {
    val c = LocalColors.current
    Column(
        Modifier.fillMaxWidth().padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Txt("A clear plan is a quieter phone.", 19f, c.fg, FontWeight.Medium)
        Txt("Add one task or class to start your day.", 13f, c.fg2, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(18.dp))
        PillButton("Add first task", onAdd, filled = true)
    }
}

@Composable
private fun PlanRow(t: PlanTask, vm: LauncherViewModel, edit: () -> Unit) {
    val c = LocalColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.tile)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (t.completed) c.accent else c.track)
                .tap { vm.updatePlanTask(t.copy(completed = !t.completed)) },
            contentAlignment = Alignment.Center
        ) {
            if (t.completed) Txt("\u2713", 13f)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Txt(
                t.title, 15f,
                if (t.completed) c.fg2 else c.fg,
                FontWeight.Medium,
                strike = t.completed
            )
            Txt(
                listOfNotNull(
                    t.className.ifBlank { null },
                    t.due.ifBlank { null },
                    if (t.priority >= 2) "Important" else null
                ).joinToString(" · ").ifBlank { "No deadline" },
                12f, c.fg2
            )
        }
        PillButton("Edit", edit, Modifier.height(48.dp))
        PillButton("Focus", { vm.startFocusFor(t) }, Modifier.height(48.dp))
    }
}

@Composable
private fun DayChooser(selected: LocalDate, select: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (-3..3).forEach { offset ->
            val d = LocalDate.now().plusDays(offset.toLong())
            PillButton(
                d.dayOfWeek.name.take(1) + d.dayOfMonth,
                { select(d) },
                filled = d == selected
            )
        }
    }
}

@Composable
private fun PlanEditor(
    task: PlanTask? = null,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (String, String, Int, String) -> Unit
) {
    val c = LocalColors.current
    var title by remember { mutableStateOf(task?.title ?: "") }
    var due by remember { mutableStateOf(task?.due ?: "") }
    var course by remember { mutableStateOf(task?.className ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: 1) }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.fg.copy(alpha = .22f))
            .tap(onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(if (c.dark) Color(0xFF171719) else Color.White)
                .padding(20.dp)
        ) {
            Txt(if (task == null) "New task" else "Edit task", 23f, c.fg, FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            GlassField(title, "Task", { title = it })
            Spacer(Modifier.height(8.dp))
            GlassField(course, "Class / course (optional)", { course = it })
            Spacer(Modifier.height(8.dp))
            GlassField(due, "Due date YYYY-MM-DD (optional)", { due = it })
            Spacer(Modifier.height(10.dp))
            Segmented(listOf(1 to "Normal", 2 to "High"), priority, onSelect = { priority = it })
            Spacer(Modifier.height(16.dp))
            Row {
                PillButton("Cancel", onDismiss)
                Spacer(Modifier.width(10.dp))
                if (onDelete != null) PillButton("Delete", onDelete)
                Spacer(Modifier.width(10.dp))
                PillButton("Save", { onSave(title, due, priority, course) }, filled = true)
            }
        }
    }
}

@Composable
private fun ClassEditor(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    val c = LocalColors.current
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var time by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.fg.copy(alpha = .22f))
            .tap(onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(if (c.dark) Color(0xFF171719) else Color.White)
                .padding(20.dp)
        ) {
            Txt("New class", 23f, c.fg, FontWeight.SemiBold)
            GlassField(title, "Class title", { title = it })
            Spacer(Modifier.height(8.dp))
            GlassField(course, "Course", { course = it })
            Spacer(Modifier.height(8.dp))
            GlassField(date, "Date YYYY-MM-DD", { date = it })
            Spacer(Modifier.height(8.dp))
            GlassField(time, "Time e.g. 10:30", { time = it })
            Spacer(Modifier.height(12.dp))
            PillButton("Save class", { onSave(title, date, time, course) }, filled = true)
        }
    }
}
