package com.worddrop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worddrop.app.data.local.Difficulty
import com.worddrop.app.data.local.WordEntity
import com.worddrop.app.ui.theme.WordDropType
import com.worddrop.app.ui.theme.wd

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = wd.faint) {
    Text(text = text.uppercase(), style = WordDropType.eyebrow, color = color, modifier = modifier)
}

@Composable
fun Difficulty.label(): String = when (this) {
    Difficulty.EVERYDAY -> "Everyday"
    Difficulty.ADVANCED -> "Advanced"
    Difficulty.RARE -> "Rare"
}

/** Pill in the tier colour on a light tint (UI/UX spec §7 TierChip). */
@Composable
fun TierChip(difficulty: Difficulty, modifier: Modifier = Modifier) {
    val color = wd.tier(difficulty)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = difficulty.label().uppercase(), style = WordDropType.chip, color = color)
    }
}

/** Outlined pill for categories and other neutral tags. */
@Composable
fun OutlineChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, wd.hairline, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = text.uppercase(), style = WordDropType.chip, color = wd.muted)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = wd.accent,
    content: Color = wd.onAccent,
    trailing: ImageVector? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(if (enabled) container else container.copy(alpha = 0.38f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = WordDropType.button, color = content)
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailing, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    tint: Color = wd.ink,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .border(1.5.dp, tint, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = WordDropType.button.copy(fontSize = 15.sp), color = tint)
    }
}

@Composable
fun TextButtonPlain(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = wd.muted) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = WordDropType.bodySmall.copy(fontWeight = FontWeight.Medium), color = color)
    }
}

/** 44dp target around a 22dp glyph (UI/UX spec §9). */
@Composable
fun WdIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = wd.ink,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = wd.hairline)
}

/** Headword + POS abbreviation, one-line definition, chevron (UI/UX spec §7 WordRow). */
@Composable
fun WordRow(
    word: WordEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = true,
    horizontalPadding: Dp = 20.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(word.word, style = WordDropType.listWord, color = wd.ink)
                Text(word.partOfSpeech.abbreviate(), style = WordDropType.caption.copy(fontSize = 13.sp), color = wd.faint, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text(word.definition, style = WordDropType.caption, color = wd.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (showChevron) {
            Icon(WdIcons.ChevronRight, contentDescription = null, tint = wd.hairline.darken(), modifier = Modifier.size(20.dp))
        }
    }
}

fun String.abbreviate(): String = when (lowercase()) {
    "adjective" -> "adj."
    "noun" -> "n."
    "verb" -> "v."
    "adverb" -> "adv."
    else -> this
}

private fun Color.darken(): Color = copy(red = red * 0.85f, green = green * 0.85f, blue = blue * 0.85f)

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = WordDropType.screenTitle.copy(fontSize = 24.sp, lineHeight = 30.sp), color = wd.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = WordDropType.body, color = wd.muted, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", WdIcons.Calendar),
    SAVED("Saved", WdIcons.Bookmark),
    QUIZ("Quiz", WdIcons.ListCheck),
}

/** Three tabs; active one gets an accent tint pill (UI/UX spec §7 BottomNav). */
@Composable
fun BottomNav(selected: Tab, onSelect: (Tab) -> Unit, tabs: List<Tab> = Tab.entries) {
    Column(modifier = Modifier.fillMaxWidth().background(wd.surface)) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEach { tab -> NavItem(tab, tab == selected) { onSelect(tab) } }
        }
    }
}

@Composable
private fun RowScope.NavItem(tab: Tab, active: Boolean, onClick: () -> Unit) {
    val tint = if (active) wd.accent else wd.muted
    Column(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) wd.accentTint else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            tab.label,
            style = if (active) WordDropType.navLabel.copy(fontWeight = FontWeight.SemiBold) else WordDropType.navLabel,
            color = tint,
        )
    }
}
