package com.worddrop.app.ui.detail

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.worddrop.app.R
import com.worddrop.app.data.local.WordEntity
import com.worddrop.app.data.repository.SavedWordsRepository
import com.worddrop.app.data.repository.WordRepository
import com.worddrop.app.ui.components.Eyebrow
import com.worddrop.app.ui.components.Hairline
import com.worddrop.app.ui.components.OutlineChip
import com.worddrop.app.ui.components.PrimaryButton
import com.worddrop.app.ui.components.TierChip
import com.worddrop.app.ui.components.WdIconButton
import com.worddrop.app.ui.components.WdIcons
import com.worddrop.app.ui.home.PhoneticRow
import com.worddrop.app.ui.navigation.Detail
import com.worddrop.app.ui.theme.WordDropType
import com.worddrop.app.ui.theme.wd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(val word: WordEntity? = null, val isSaved: Boolean = false, val loaded: Boolean = false)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val words: WordRepository,
    private val saved: SavedWordsRepository,
) : ViewModel() {
    private val wordId: String = savedState.toRoute<Detail>().wordId

    val state = combine(words.observeWord(wordId), saved.observeIsSaved(wordId)) { w, s -> DetailUiState(w, s, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun toggleSave() {
        val s = state.value
        viewModelScope.launch { if (s.isSaved) saved.remove(wordId) else saved.save(wordId) }
    }

    /** "Next word" on Detail advances the shared word and returns its id, so the screen can swap to it. */
    fun nextWord(onPicked: (String) -> Unit) {
        viewModelScope.launch { words.nextWord()?.let { onPicked(it.id) } }
    }

    /** Synonym chips open Detail only when the synonym is itself in the bank (UI/UX spec §5). */
    fun openSynonym(text: String, onFound: (String) -> Unit) {
        viewModelScope.launch { words.findByWord(text)?.let { onFound(it.id) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    wordId: String,
    onBack: () -> Unit,
    onOpenWord: (String) -> Unit,
    vm: DetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMsg = stringResource(R.string.action_saved)
    val removedMsg = stringResource(R.string.removed_from_saved)
    val undoLabel = stringResource(R.string.action_undo)

    Box(modifier = Modifier.fillMaxSize().background(wd.paper)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                WdIconButton(WdIcons.Back, stringResource(R.string.action_back), onBack)
                if (state.word != null) {
                    WdIconButton(
                        icon = if (state.isSaved) WdIcons.BookmarkFilled else WdIcons.Bookmark,
                        contentDescription = stringResource(if (state.isSaved) R.string.action_saved else R.string.action_save),
                        tint = if (state.isSaved) wd.accent else wd.ink,
                        onClick = {
                            val wasSaved = state.isSaved
                            vm.toggleSave()
                            scope.launch {
                                if (wasSaved) {
                                    val r = snackbar.showSnackbar(removedMsg, actionLabel = undoLabel)
                                    if (r == SnackbarResult.ActionPerformed) vm.toggleSave()
                                } else {
                                    snackbar.showSnackbar(savedMsg)
                                }
                            }
                        },
                    )
                }
            }

            val word = state.word
            if (word != null) {
                Body(word, onOpenWord = onOpenWord, openSynonym = { vm.openSynonym(it, onOpenWord) }, modifier = Modifier.weight(1f))
                Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    PrimaryButton(
                        text = stringResource(R.string.action_next_word),
                        onClick = { vm.nextWord(onOpenWord) },
                        trailing = WdIcons.ChevronRight,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else if (state.loaded) {
                // Deep link to an id that isn't in the bank (e.g. a stale widget after a bank update).
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This word isn't in your word bank.", style = WordDropType.body, color = wd.muted)
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp)) { data ->
            Snackbar(snackbarData = data, containerColor = wd.ink, contentColor = wd.paper, actionColor = wd.accent, shape = RoundedCornerShape(14.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Body(word: WordEntity, onOpenWord: (String) -> Unit, openSynonym: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(word.word, style = WordDropType.headwordDetail, color = wd.ink)
            word.phonetic?.let { PhoneticRow(it, word.word, large = true) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(word.partOfSpeech.replaceFirstChar { it.titlecase() }, style = WordDropType.caption, color = wd.muted)
                Box(Modifier.size(3.dp).clip(RoundedCornerShape(2.dp)).background(wd.hairline))
                TierChip(word.difficulty)
                word.category?.let { OutlineChip(it) }
            }
        }

        Section(stringResource(R.string.detail_definition)) {
            Text(word.definition, style = WordDropType.definition, color = wd.ink)
        }
        Section(stringResource(R.string.detail_example)) {
            Text(word.exampleSentence, style = WordDropType.example, color = wd.muted)
        }
        if (word.synonyms.isNotEmpty()) {
            Section(stringResource(R.string.detail_synonyms)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    word.synonyms.forEach { s -> SynonymChip(s) { openSynonym(s) } }
                }
            }
        }
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Hairline()
        Spacer(Modifier.height(10.dp))
        Eyebrow(label)
        content()
    }
}

@Composable
private fun SynonymChip(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(wd.surface)
            .border(1.dp, wd.hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, style = WordDropType.optionWord.copy(fontSize = 16.sp), color = wd.ink)
    }
}
