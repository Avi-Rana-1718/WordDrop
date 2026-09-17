package com.worddrop.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.worddrop.app.R
import com.worddrop.app.data.local.Difficulty

/** Editorial palette (UI/UX spec §6.1). Values mirror res/values(-night)/colors.xml. */
@Immutable
data class WordDropColors(
    val paper: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val hairline: Color,
    val accent: Color,
    val onAccent: Color,
    val accentTint: Color,
    val everyday: Color,
    val advanced: Color,
    val rare: Color,
) {
    fun tier(d: Difficulty): Color = when (d) {
        Difficulty.EVERYDAY -> everyday
        Difficulty.ADVANCED -> advanced
        Difficulty.RARE -> rare
    }
}

val LightColors = WordDropColors(
    paper = Color(0xFFF6F1E8),
    surface = Color(0xFFFFFDF9),
    ink = Color(0xFF1C1917),
    muted = Color(0xFF6B645C),
    faint = Color(0xFF8A8177),
    hairline = Color(0xFFE3DCD0),
    accent = Color(0xFF9A3B2E),
    onAccent = Color(0xFFFFFDF9),
    accentTint = Color(0x1A9A3B2E),
    everyday = Color(0xFF5F7A5A),
    advanced = Color(0xFFB07A2A),
    rare = Color(0xFF6D4A7A),
)

val DarkColors = WordDropColors(
    paper = Color(0xFF1A1714),
    surface = Color(0xFF24201C),
    ink = Color(0xFFEFE8DC),
    muted = Color(0xFFA39A8D),
    faint = Color(0xFF8E857A),
    hairline = Color(0xFF3A342E),
    accent = Color(0xFFD0705F),
    onAccent = Color(0xFF1A1714),
    accentTint = Color(0x24D0705F),
    everyday = Color(0xFF8FAE89),
    advanced = Color(0xFFD2A15A),
    rare = Color(0xFFA98AB8),
)

val LocalWordDropColors = staticCompositionLocalOf { LightColors }

/** Newsreader: anything that *is* a word. Source Sans 3: UI (UI/UX spec §6.2). */
val Newsreader = FontFamily(
    Font(R.font.newsreader, FontWeight.Normal, variationSettings = FontVariation.Settings(FontWeight.Normal, FontStyle.Normal)),
    Font(R.font.newsreader, FontWeight.Medium, variationSettings = FontVariation.Settings(FontWeight.Medium, FontStyle.Normal)),
    Font(R.font.newsreader, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontWeight.SemiBold, FontStyle.Normal)),
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic, variationSettings = FontVariation.Settings(FontWeight.Normal, FontStyle.Italic)),
)

val SourceSans = FontFamily(
    Font(R.font.source_sans_3, FontWeight.Normal, variationSettings = FontVariation.Settings(FontWeight.Normal, FontStyle.Normal)),
    Font(R.font.source_sans_3, FontWeight.Medium, variationSettings = FontVariation.Settings(FontWeight.Medium, FontStyle.Normal)),
    Font(R.font.source_sans_3, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontWeight.SemiBold, FontStyle.Normal)),
)

/** Named roles from the spec's type scale, on top of Material's slots. */
object WordDropType {
    val headwordDetail = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 52.sp, lineHeight = 52.sp, letterSpacing = (-0.02).em)
    val headwordToday = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 46.sp, lineHeight = 47.sp, letterSpacing = (-0.015).em)
    val screenTitle = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 33.sp, letterSpacing = (-0.01).em)
    val quizPrompt = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 35.sp, letterSpacing = (-0.01).em)
    val listWord = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 26.sp)
    val optionWord = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 24.sp)
    val example = TextStyle(fontFamily = Newsreader, fontStyle = FontStyle.Italic, fontSize = 18.sp, lineHeight = 26.sp)
    val exampleSmall = TextStyle(fontFamily = Newsreader, fontStyle = FontStyle.Italic, fontSize = 16.sp, lineHeight = 22.sp)
    val definition = TextStyle(fontFamily = SourceSans, fontSize = 18.sp, lineHeight = 26.sp)
    val definitionSmall = TextStyle(fontFamily = SourceSans, fontSize = 17.sp, lineHeight = 24.sp)
    val body = TextStyle(fontFamily = SourceSans, fontSize = 16.sp, lineHeight = 22.sp)
    val bodySmall = TextStyle(fontFamily = SourceSans, fontSize = 15.sp, lineHeight = 21.sp)
    val caption = TextStyle(fontFamily = SourceSans, fontSize = 14.sp, lineHeight = 19.sp)
    val button = TextStyle(fontFamily = SourceSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp)
    val eyebrow = TextStyle(fontFamily = SourceSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.12.em)
    val chip = TextStyle(fontFamily = SourceSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.06.em)
    val navLabel = TextStyle(fontFamily = SourceSans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
}

private val MaterialType = Typography(
    displayLarge = WordDropType.headwordDetail,
    headlineMedium = WordDropType.screenTitle,
    titleLarge = WordDropType.listWord,
    bodyLarge = WordDropType.definition,
    bodyMedium = WordDropType.body,
    bodySmall = WordDropType.caption,
    labelLarge = WordDropType.button,
    labelSmall = WordDropType.eyebrow,
)

@Composable
fun WordDropTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.paper, onBackground = colors.ink,
            surface = colors.surface, onSurface = colors.ink, onSurfaceVariant = colors.muted,
            outline = colors.hairline, outlineVariant = colors.hairline,
            inverseSurface = colors.ink, inverseOnSurface = colors.paper,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.paper, onBackground = colors.ink,
            surface = colors.surface, onSurface = colors.ink, onSurfaceVariant = colors.muted,
            outline = colors.hairline, outlineVariant = colors.hairline,
            inverseSurface = colors.ink, inverseOnSurface = colors.paper,
        )
    }
    CompositionLocalProvider(LocalWordDropColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = MaterialType, content = content)
    }
}

/** Shorthand for the current palette inside composables. */
val wd: WordDropColors
    @Composable get() = LocalWordDropColors.current
