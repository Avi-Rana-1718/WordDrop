package com.worddrop.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.worddrop.app.R
import com.worddrop.app.data.local.Difficulty
import com.worddrop.app.data.prefs.RefreshInterval
import com.worddrop.app.data.prefs.Settings
import com.worddrop.app.data.prefs.UserPreferences
import com.worddrop.app.data.prefs.WordFilter
import com.worddrop.app.data.repository.WordRepository
import com.worddrop.app.ui.components.Eyebrow
import com.worddrop.app.ui.components.Hairline
import com.worddrop.app.ui.components.WdIconButton
import com.worddrop.app.ui.components.WdIcons
import com.worddrop.app.ui.components.label
import com.worddrop.app.ui.theme.WordDropType
import com.worddrop.app.ui.theme.wd
import com.worddrop.app.widget.WidgetScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(val settings: Settings? = null, val categories: List<String> = emptyList())

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val scheduler: WidgetScheduler,
    words: WordRepository,
) : ViewModel() {
    val state = combine(prefs.settings, words.observeCategories()) { s, c -> SettingsUiState(s, c) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setInterval(interval: RefreshInterval) = viewModelScope.launch {
        prefs.setRefreshInterval(interval)
        scheduler.reschedule(interval)
    }

    /** Returns false (and changes nothing) when the tap would switch off the last tier. */
    fun toggleTier(tier: Difficulty): Boolean {
        val current = state.value.settings?.filter?.difficulties ?: return false
        val next = if (tier in current) current - tier else current + tier
        if (next.isEmpty()) return false
        viewModelScope.launch { prefs.setDifficulties(next) }
        return true
    }

    fun toggleCategory(category: String) = viewModelScope.launch {
        val current = state.value.settings?.filter?.categories ?: return@launch
        prefs.setCategories(if (category in current) current - category else current + category)
    }

    fun clearCategories() = viewModelScope.launch { prefs.setCategories(emptySet()) }
    fun setReminder(enabled: Boolean) = viewModelScope.launch { prefs.setReminderEnabled(enabled) }
    fun dismissBatteryTip() = viewModelScope.launch { prefs.dismissBatteryTip() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings = state.settings
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val keepOne = stringResource(R.string.settings_keep_one_tier)
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(wd.paper)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WdIconButton(WdIcons.Back, stringResource(R.string.action_back), onBack)
                Text(stringResource(R.string.settings_title), style = WordDropType.screenTitle.copy(fontSize = 26.sp), color = wd.ink)
            }

            if (settings == null) return@Column

            Column(
                modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Group(stringResource(R.string.settings_refresh)) {
                    Segmented(
                        options = RefreshInterval.entries.map { it to it.label() },
                        selected = settings.refreshInterval,
                        onSelect = vm::setInterval,
                    )
                }

                Group(stringResource(R.string.settings_difficulty)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Difficulty.entries.forEach { tier ->
                            FilterChip(
                                text = tier.label(),
                                selected = tier in settings.filter.difficulties,
                                color = wd.tier(tier),
                                onClick = {
                                    if (!vm.toggleTier(tier)) scope.launch { snackbar.showSnackbar(keepOne) }
                                },
                            )
                        }
                    }
                }

                Group(stringResource(R.string.settings_content)) {
                    Column {
                        Hairline()
                        Text(
                            stringResource(R.string.settings_categories),
                            style = WordDropType.body,
                            color = wd.ink,
                            modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp),
                        ) {
                            FilterChip(
                                text = stringResource(R.string.settings_categories_all),
                                selected = settings.filter.categories.isEmpty(),
                                color = wd.ink,
                                onClick = vm::clearCategories,
                            )
                            (state.categories + WordFilter.GENERAL).forEach { c ->
                                FilterChip(
                                    text = c.replaceFirstChar { it.titlecase() },
                                    selected = c in settings.filter.categories,
                                    color = wd.ink,
                                    onClick = { vm.toggleCategory(c) },
                                )
                            }
                        }
                        Hairline()
                        SettingRow(stringResource(R.string.settings_theme), stringResource(R.string.settings_theme_system))
                        Hairline()
                    }
                }

                // Daily reminder (Phase 2) is hidden until notifications are actually wired up:
                // the toggle only persisted a preference and nothing fired. Restore the Reminders
                // group here — and POST_NOTIFICATIONS in the manifest — when it does.

                if (!settings.batteryTipDismissed) {
                    BatteryTip(
                        onDismiss = vm::dismissBatteryTip,
                        onOpen = {
                            val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            runCatching { context.startActivity(intent) }
                        },
                    )
                }
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) { data ->
            Snackbar(snackbarData = data, containerColor = wd.ink, contentColor = wd.paper, shape = RoundedCornerShape(14.dp))
        }
    }
}

@Composable
private fun RefreshInterval.label(): String = when (this) {
    RefreshInterval.H4 -> stringResource(R.string.settings_refresh_4h)
    RefreshInterval.H12 -> stringResource(R.string.settings_refresh_12h)
    RefreshInterval.H24 -> stringResource(R.string.settings_refresh_24h)
}

@Composable
private fun Group(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow(label)
        content()
    }
}

/** Equal segments in a 4dp tray; the selected one is a raised surface (UI/UX spec §7). */
@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(wd.hairline.copy(alpha = 0.6f))
            .padding(4.dp),
    ) {
        options.forEach { (value, text) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .then(if (active) Modifier.shadow(1.dp, RoundedCornerShape(10.dp)) else Modifier)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) wd.surface else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text,
                    style = WordDropType.bodySmall.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (active) wd.ink else wd.muted,
                )
            }
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.12f) else wd.surface)
            .border(1.5.dp, if (selected) color else wd.hairline, shape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (selected) Icon(WdIcons.Check, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(
            text,
            style = WordDropType.bodySmall.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) color else wd.muted,
        )
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = WordDropType.body, color = wd.ink, modifier = Modifier.weight(1f))
        Text(value, style = WordDropType.bodySmall, color = wd.muted)
        Spacer(Modifier.width(6.dp))
        Icon(WdIcons.ChevronRight, contentDescription = null, tint = wd.hairline, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BatteryTip(onDismiss: () -> Unit, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier.fillMaxWidth().clip(shape).background(wd.surface).border(1.dp, wd.hairline, shape).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                stringResource(R.string.settings_battery_title),
                style = WordDropType.listWord.copy(fontSize = 18.sp),
                color = wd.ink,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(WdIcons.Close, contentDescription = "Dismiss", tint = wd.faint, modifier = Modifier.size(18.dp))
            }
        }
        Text(stringResource(R.string.settings_battery_body), style = WordDropType.caption, color = wd.muted)
        Text(
            stringResource(R.string.action_open_battery_settings),
            style = WordDropType.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = wd.accent,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onOpen).padding(vertical = 4.dp),
        )
    }
}
