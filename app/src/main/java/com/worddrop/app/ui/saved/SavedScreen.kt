package com.worddrop.app.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.worddrop.app.R
import com.worddrop.app.data.local.WordEntity
import com.worddrop.app.data.repository.SavedWordsRepository
import com.worddrop.app.ui.components.BottomNav
import com.worddrop.app.ui.components.EmptyState
import com.worddrop.app.ui.components.Eyebrow
import com.worddrop.app.ui.components.Hairline
import com.worddrop.app.ui.components.Tab
import com.worddrop.app.ui.components.WdIcons
import com.worddrop.app.ui.components.WordRow
import com.worddrop.app.ui.theme.WordDropType
import com.worddrop.app.ui.theme.wd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(private val saved: SavedWordsRepository) : ViewModel() {
    val words = saved.observeSaved().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(wordId: String) = viewModelScope.launch { saved.remove(wordId) }
    fun restore(wordId: String) = viewModelScope.launch { saved.save(wordId) }
}

@Composable
fun SavedScreen(onOpenWord: (String) -> Unit, onTab: (Tab) -> Unit, vm: SavedViewModel = hiltViewModel()) {
    val words by vm.words.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.action_undo)

    Box(modifier = Modifier.fillMaxSize().background(wd.paper)) {
        Column(modifier = Modifier.fillMaxSize()) {
            val list = words
            Column(modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 16.dp)) {
                Eyebrow(
                    when {
                        list == null -> ""
                        list.size == 1 -> stringResource(R.string.saved_count_one)
                        else -> stringResource(R.string.saved_count, list.size)
                    },
                )
                Text(stringResource(R.string.tab_saved), style = WordDropType.screenTitle, color = wd.ink)
            }
            when {
                list == null -> Box(Modifier.weight(1f))
                list.isEmpty() -> EmptyState(
                    title = stringResource(R.string.saved_empty_title),
                    body = stringResource(R.string.saved_empty_body),
                    modifier = Modifier.weight(1f),
                )
                else -> LazyColumn(modifier = Modifier.weight(1f)) {
                    items(list, key = { it.id }) { word ->
                        val removedMsg = stringResource(R.string.removed_word, word.word)
                        SwipeRow(
                            word = word,
                            onOpen = { onOpenWord(word.id) },
                            onRemove = {
                                vm.remove(word.id)
                                scope.launch {
                                    val r = snackbar.showSnackbar(removedMsg, actionLabel = undoLabel)
                                    if (r == SnackbarResult.ActionPerformed) vm.restore(word.id)
                                }
                            },
                        )
                    }
                    item { Hairline() }
                }
            }
            BottomNav(selected = Tab.SAVED, onSelect = onTab)
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)) { data ->
            Snackbar(snackbarData = data, containerColor = wd.ink, contentColor = wd.paper, actionColor = wd.accent, shape = RoundedCornerShape(14.dp))
        }
    }
}

/** Swipe left reveals a 96dp accent panel with trash + "Remove"; release past half removes (UI/UX spec §5). */
@Composable
private fun SwipeRow(word: WordEntity, onOpen: () -> Unit, onRemove: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { it == SwipeToDismissBoxValue.EndToStart },
        positionalThreshold = { total -> total * 0.5f },
    )
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) onRemove()
    }
    Column {
        Hairline()
        SwipeToDismissBox(
            state = state,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                Box(modifier = Modifier.fillMaxSize().background(wd.accent), contentAlignment = Alignment.CenterEnd) {
                    Column(
                        modifier = Modifier.width(96.dp).fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(WdIcons.Trash, contentDescription = null, tint = wd.onAccent, modifier = Modifier.size(22.dp))
                        Box(Modifier.height(4.dp))
                        Text(stringResource(R.string.action_remove), style = WordDropType.navLabel, color = wd.onAccent)
                    }
                }
            },
        ) {
            Box(Modifier.fillMaxWidth().background(wd.paper)) {
                WordRow(word, onClick = onOpen)
            }
        }
    }
}
