package com.worddrop.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.compose.ui.graphics.Color
import com.worddrop.app.R
import com.worddrop.app.data.local.WordEntity
import com.worddrop.app.data.repository.WidgetNotifier
import com.worddrop.app.data.repository.WordRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Glance classes are instantiated by the system, so they reach Hilt through an entry point. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun wordRepository(): WordRepository
}

/** What the widget renders. Decoupled from Room so the fallback word needs no database. */
data class WidgetWord(
    val id: String,
    val word: String,
    val phonetic: String?,
    val partOfSpeech: String,
    val definition: String,
    val example: String,
) {
    companion object {
        /**
         * Shown when the widget is placed before the word bank is ready (UI/UX spec §3.4).
         * Must be a real entry in word_bank.json so its tap target resolves.
         */
        val FALLBACK = WidgetWord(
            id = "ephemeral",
            word = "ephemeral",
            phonetic = "/əˈfem(ə)rəl/",
            partOfSpeech = "adjective",
            definition = "Lasting for a very short time.",
            example = "The ephemeral beauty of cherry blossoms draws crowds each spring.",
        )
    }
}

fun WordEntity.toWidgetWord() = WidgetWord(
    id = id,
    word = word,
    phonetic = phonetic,
    partOfSpeech = partOfSpeech,
    definition = definition,
    example = exampleSentence,
)

class WordDropWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WordDropWidget()
}

/** Tapping the refresh glyph: advance in-process, no WorkManager round trip (Tech §3.3). */
class WidgetRefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).wordRepository()
        runCatching { repo.nextWord() }.onFailure { Log.w(TAG, "Manual refresh failed", it) }
    }

    private companion object { const val TAG = "WidgetRefreshAction" }
}

@Singleton
class GlanceWidgetNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetNotifier {
    override suspend fun widgetContentChanged() {
        runCatching { WordDropWidget().updateAll(context) }
            .onFailure { Log.w("GlanceWidgetNotifier", "updateAll failed", it) }
    }
}

class WordDropWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(COMPACT, EXPANDED))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).wordRepository()
        val initial = runCatching { repo.currentWord() }
            .onFailure { Log.w(TAG, "Falling back to bundled word", it) }
            .getOrNull()
            ?.toWidgetWord()
            ?: WidgetWord.FALLBACK
        // Glance keeps this composition alive for a while after the last render; updateAll() on a
        // live session recomposes but does not re-run provideGlance. Observing the repository here
        // is what makes a refresh within that window actually show the new word.
        provideContent {
            val word by remember(repo) { repo.observeCurrentWord().map { it?.toWidgetWord() ?: initial } }
                .collectAsState(initial)
            WidgetContent(word)
        }
    }

    companion object {
        private const val TAG = "WordDropWidget"

        /** 2x1 — word + part of speech. */
        val COMPACT = DpSize(110.dp, 40.dp)

        /** 3x2 — word, phonetic, definition, example, refresh. */
        val EXPANDED = DpSize(180.dp, 110.dp)
    }
}

@Composable
private fun WidgetContent(word: WidgetWord) {
    val context = LocalContext.current
    val size = LocalSize.current
    val openDetail = actionStartActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse("worddrop://word/${word.id}?fromWidget=true"))
            .setPackage(context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .clickable(openDetail),
    ) {
        if (size.width >= WordDropWidget.EXPANDED.width && size.height >= WordDropWidget.EXPANDED.height) {
            ExpandedLayout(word)
        } else {
            CompactLayout(word)
        }
    }
}

// Day/night pairs mirror res/values(-night)/colors.xml (UI/UX spec §6.1).
private val ink = ColorProvider(day = Color(0xFF1C1917), night = Color(0xFFEFE8DC))
private val muted = ColorProvider(day = Color(0xFF6B645C), night = Color(0xFFA39A8D))
private val faint = ColorProvider(day = Color(0xFF8A8177), night = Color(0xFF8E857A))

private val labelStyle = TextStyle(color = faint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
private val phoneticStyle = TextStyle(color = muted, fontSize = 14.sp)
private val definitionStyle = TextStyle(color = ink, fontSize = 15.sp)
private val exampleStyle = TextStyle(color = muted, fontSize = 14.sp, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)

private fun wordStyle(size: Int) =
    TextStyle(color = ink, fontSize = size.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Serif)

@Composable
private fun CompactLayout(word: WidgetWord) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(text = word.partOfSpeech.uppercase(), style = labelStyle, maxLines = 1)
        Spacer(GlanceModifier.height(2.dp))
        Text(text = word.word, style = wordStyle(if (word.word.length > 11) 22 else 28), maxLines = 1)
    }
}

@Composable
private fun ExpandedLayout(word: WidgetWord) {
    val context = LocalContext.current
    val size = LocalSize.current
    val roomy = size.height >= 150.dp
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(start = 20.dp, top = 14.dp, end = 10.dp, bottom = 16.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = "WORDDROP · ${word.partOfSpeech.uppercase()}",
                style = labelStyle,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = context.getString(R.string.action_new_word),
                colorFilter = ColorFilter.tint(muted),
                modifier = GlanceModifier.size(44.dp).padding(12.dp).clickable(actionRunCallback<WidgetRefreshAction>()),
            )
        }
        Text(text = word.word, style = wordStyle(if (word.word.length > 11) 28 else 34), maxLines = 1)
        word.phonetic?.let {
            Spacer(GlanceModifier.height(2.dp))
            Text(text = it, style = phoneticStyle, maxLines = 1)
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(text = word.definition, style = definitionStyle, maxLines = 2, modifier = GlanceModifier.padding(end = 8.dp))
        if (roomy) {
            Spacer(GlanceModifier.height(4.dp))
            Text(text = word.example, style = exampleStyle, maxLines = 2, modifier = GlanceModifier.padding(end = 8.dp))
        }
    }
}
