package com.worddrop.app.ui.home

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.worddrop.app.R
import com.worddrop.app.data.local.WordEntity
import com.worddrop.app.data.prefs.UserPreferences
import com.worddrop.app.data.repository.SavedWordsRepository
import com.worddrop.app.data.repository.WordRepository
import com.worddrop.app.data.seed.SeedFormatException
import com.worddrop.app.ui.components.BottomNav
import com.worddrop.app.ui.components.EmptyState
import com.worddrop.app.ui.components.Eyebrow
import com.worddrop.app.ui.components.Hairline
import com.worddrop.app.ui.components.PrimaryButton
import com.worddrop.app.ui.components.SecondaryButton
import com.worddrop.app.ui.components.Tab
import com.worddrop.app.ui.components.TierChip
import com.worddrop.app.ui.components.WdIconButton
import com.worddrop.app.ui.components.WdIcons
import com.worddrop.app.ui.components.WordRow
import com.worddrop.app.ui.theme.WordDropType
import com.worddrop.app.ui.theme.wd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data class Ready(val word: WordEntity, val isSaved: Boolean, val earlier: List<WordEntity>) : TodayUiState
    data object NoMatch : TodayUiState
    data class Broken(val message: String?) : TodayUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val words: WordRepository,
    private val saved: SavedWordsRepository,
    prefs: UserPreferences,
) : ViewModel() {

    private val seedError = MutableStateFlow<String?>(null)
    private val noMatch = MutableStateFlow(false)

    init {
        // Make sure a word exists before the screen first renders. Also picks up a due refresh.
        viewModelScope.launch {
            try {
                words.refreshIfDue()
                noMatch.value = words.currentWord() == null
            } catch (e: SeedFormatException) {
                seedError.value = e.message
            }
        }
    }

    val state: StateFlow<TodayUiState> = combine(
        words.observeCurrentWord().flatMapLatest { word ->
            if (word == null) flowOf(null)
            else combine(saved.observeIsSaved(word.id), words.observeRecentlyShown(EARLIER_COUNT)) { s, e -> Triple(word, s, e) }
        },
        seedError,
        noMatch,
        prefs.settings,
    ) { triple, error, none, _ ->
        when {
            error != null -> TodayUiState.Broken(error)
            triple != null -> TodayUiState.Ready(triple.first, triple.second, triple.third)
            none -> TodayUiState.NoMatch
            else -> TodayUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    fun toggleSave(word: WordEntity, isSaved: Boolean) {
        viewModelScope.launch { if (isSaved) saved.remove(word.id) else saved.save(word.id) }
    }

    fun nextWord() {
        viewModelScope.launch {
            val picked = runCatching { words.nextWord() }.getOrNull()
            noMatch.value = picked == null
        }
    }

    fun retry() {
        seedError.value = null
        viewModelScope.launch {
            try {
                noMatch.value = words.currentWord() == null
            } catch (e: SeedFormatException) {
                seedError.value = e.message
            }
        }
    }

    companion object { const val EARLIER_COUNT = 3 }
}

@Composable
fun TodayScreen(
    onOpenWord: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onTab: (Tab) -> Unit,
    vm: TodayViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().background(wd.paper)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header(onOpenSettings)
            when (val s = state) {
                TodayUiState.Loading -> Spacer(Modifier.height(320.dp))
                is TodayUiState.Ready -> {
                    WordCard(
                        word = s.word,
                        isSaved = s.isSaved,
                        onToggleSave = { vm.toggleSave(s.word, s.isSaved) },
                        onNext = vm::nextWord,
                        onOpen = { onOpenWord(s.word.id) },
                    )
                    if (s.earlier.isNotEmpty()) EarlierList(s.earlier, onOpenWord)
                }
                TodayUiState.NoMatch -> EmptyState(
                    title = stringResource(R.string.error_no_match_title),
                    body = "",
                    modifier = Modifier.height(360.dp),
                    action = { PrimaryButton(stringResource(R.string.action_change_filters), onOpenSettings) },
                )
                is TodayUiState.Broken -> EmptyState(
                    title = stringResource(R.string.error_seed_title),
                    body = stringResource(R.string.error_seed_body),
                    modifier = Modifier.height(360.dp),
                    action = { PrimaryButton(stringResource(R.string.action_try_again), vm::retry) },
                )
            }
        }
        BottomNav(selected = Tab.TODAY, onSelect = onTab)
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Eyebrow(date)
            Text(stringResource(R.string.tab_today), style = WordDropType.screenTitle, color = wd.ink)
        }
        WdIconButton(WdIcons.Sliders, stringResource(R.string.action_settings), onOpenSettings, tint = wd.muted)
    }
}

@Composable
private fun WordCard(
    word: WordEntity,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(wd.surface)
            .border(1.dp, wd.hairline, shape)
            .clickable(onClick = onOpen)
            .padding(horizontal = 22.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TierChip(word.difficulty)
            Text(word.partOfSpeech.replaceFirstChar { it.titlecase() }, style = WordDropType.caption.copy(fontSize = 13.sp), color = wd.faint)
        }
        Text(word.word, style = WordDropType.headwordToday, color = wd.ink)
        word.phonetic?.let { PhoneticRow(it, word.word) }
        Text(word.definition, style = WordDropType.definitionSmall, color = wd.ink, modifier = Modifier.padding(top = 4.dp))
        Text(word.exampleSentence, style = WordDropType.exampleSmall, color = wd.muted)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = stringResource(if (isSaved) R.string.action_saved else R.string.action_save),
                onClick = onToggleSave,
                leading = if (isSaved) WdIcons.BookmarkFilled else WdIcons.Bookmark,
                tint = if (isSaved) wd.accent else wd.ink,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = stringResource(R.string.action_next_word),
                onClick = onNext,
                trailing = WdIcons.ChevronRight,
                modifier = Modifier.weight(1f).height(48.dp),
            )
        }
    }
}

/** Phonetic string plus a speaker glyph that reads the word aloud; hidden when TTS is unavailable. */
@Composable
fun PhoneticRow(phonetic: String, word: String, large: Boolean = false) {
    val context = LocalContext.current
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS && engine != null }
        engine
    }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(phonetic, style = if (large) WordDropType.body else WordDropType.bodySmall, color = wd.muted)
        if (ttsReady) {
            WdIconButton(
                icon = WdIcons.Speaker,
                contentDescription = stringResource(R.string.detail_pronounce, word),
                onClick = { tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, word) },
                tint = wd.accent,
            )
        }
    }
}

@Composable
private fun EarlierList(words: List<WordEntity>, onOpenWord: (String) -> Unit) {
    Column {
        Eyebrow(stringResource(R.string.today_earlier), modifier = Modifier.padding(top = 6.dp, bottom = 10.dp))
        words.forEach { w ->
            Hairline()
            WordRow(w, onClick = { onOpenWord(w.id) }, showChevron = false, horizontalPadding = 0.dp)
        }
    }
}
