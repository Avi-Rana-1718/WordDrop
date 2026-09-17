package com.worddrop.app.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.worddrop.app.R
import com.worddrop.app.data.local.WordEntity
import com.worddrop.app.data.repository.QuizQuestion
import com.worddrop.app.data.repository.QuizRepository
import com.worddrop.app.data.repository.SavedWordsRepository
import com.worddrop.app.ui.components.BottomNav
import com.worddrop.app.ui.components.EmptyState
import com.worddrop.app.ui.components.Eyebrow
import com.worddrop.app.ui.components.Hairline
import com.worddrop.app.ui.components.PrimaryButton
import com.worddrop.app.ui.components.Tab
import com.worddrop.app.ui.components.WdIcons
import com.worddrop.app.ui.components.WordRow
import com.worddrop.app.ui.theme.WordDropType
import com.worddrop.app.ui.theme.wd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface QuizUiState {
    data object Loading : QuizUiState
    data object NotEnoughWords : QuizUiState
    data class InProgress(
        val questions: List<QuizQuestion>,
        val index: Int,
        /** Id of the option the user tapped, or null before answering. */
        val selectedId: String? = null,
        val correctCount: Int = 0,
        val missed: List<WordEntity> = emptyList(),
    ) : QuizUiState {
        val question: QuizQuestion get() = questions[index]
        val answered: Boolean get() = selectedId != null
        val isCorrect: Boolean get() = selectedId == question.answer.id
        val isLast: Boolean get() = index == questions.lastIndex
    }
    data class Finished(val total: Int, val correctCount: Int, val missed: List<WordEntity>) : QuizUiState
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quiz: QuizRepository,
    private val saved: SavedWordsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    init { start() }

    fun start() {
        _state.value = QuizUiState.Loading
        viewModelScope.launch {
            val questions = quiz.buildQuiz()
            _state.value = if (questions.isEmpty()) QuizUiState.NotEnoughWords else QuizUiState.InProgress(questions, 0)
        }
    }

    fun choose(optionId: String) {
        val s = _state.value as? QuizUiState.InProgress ?: return
        if (s.answered) return
        val correct = optionId == s.question.answer.id
        _state.value = s.copy(
            selectedId = optionId,
            correctCount = s.correctCount + if (correct) 1 else 0,
            missed = if (correct) s.missed else s.missed + s.question.answer,
        )
        viewModelScope.launch { quiz.record(s.question.answer.id, correct) }
    }

    fun next() {
        _state.update { s ->
            if (s !is QuizUiState.InProgress || !s.answered) return@update s
            if (s.isLast) QuizUiState.Finished(s.questions.size, s.correctCount, s.missed)
            else s.copy(index = s.index + 1, selectedId = null)
        }
    }

    fun save(wordId: String) = viewModelScope.launch { saved.save(wordId) }
}

@Composable
fun QuizScreen(onOpenWord: (String) -> Unit, onTab: (Tab) -> Unit, vm: QuizViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().background(wd.paper)) {
        Box(modifier = Modifier.weight(1f)) {
            when (val s = state) {
                QuizUiState.Loading -> Unit
                QuizUiState.NotEnoughWords -> Column(Modifier.fillMaxSize()) {
                    Title(progress = null)
                    EmptyState(
                        title = stringResource(R.string.quiz_empty_title),
                        body = stringResource(R.string.quiz_empty_body),
                        modifier = Modifier.weight(1f),
                    )
                }
                is QuizUiState.InProgress -> Question(s, onChoose = vm::choose, onNext = vm::next)
                is QuizUiState.Finished -> Summary(s, onOpenWord = onOpenWord, onSave = vm::save, onDone = vm::start)
            }
        }
        BottomNav(selected = Tab.QUIZ, onSelect = onTab)
    }
}

@Composable
private fun Title(progress: Pair<Int, Int>?) {
    Column(
        modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp).padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.tab_quiz), style = WordDropType.screenTitle, color = wd.ink, modifier = Modifier.weight(1f))
            if (progress != null) {
                Text(
                    stringResource(R.string.quiz_progress, progress.first, progress.second),
                    style = WordDropType.eyebrow.copy(fontSize = 13.sp, letterSpacing = 0.08.em),
                    color = wd.faint,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        if (progress != null) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(wd.hairline)) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.first / progress.second.toFloat())
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(wd.accent),
                )
            }
        }
    }
}

@Composable
private fun Question(s: QuizUiState.InProgress, onChoose: (String) -> Unit, onNext: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Title(progress = (s.index + 1) to s.questions.size)
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 32.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Eyebrow(stringResource(R.string.quiz_prompt))
                Text(s.question.answer.definition, style = WordDropType.quizPrompt, color = wd.ink)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                s.question.options.forEach { option ->
                    Option(
                        word = option,
                        state = when {
                            !s.answered -> OptionState.Default
                            option.id == s.question.answer.id -> OptionState.Correct
                            option.id == s.selectedId -> OptionState.Wrong
                            else -> OptionState.Locked
                        },
                        onClick = { onChoose(option.id) },
                    )
                }
            }
            if (s.answered && !s.isCorrect) {
                val picked = s.question.options.first { it.id == s.selectedId }
                Feedback(picked)
            }
            Spacer(Modifier.height(4.dp))
            PrimaryButton(
                text = stringResource(R.string.action_continue),
                onClick = onNext,
                enabled = s.answered,
                container = wd.ink,
                content = wd.paper,
                trailing = WdIcons.ChevronRight,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private enum class OptionState { Default, Correct, Wrong, Locked }

/** Icon + colour, never colour alone (UI/UX spec §9). */
@Composable
private fun Option(word: WordEntity, state: OptionState, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val (border, fill, text) = when (state) {
        OptionState.Default, OptionState.Locked -> Triple(wd.hairline, wd.surface, wd.ink)
        OptionState.Correct -> Triple(wd.everyday, wd.everyday.copy(alpha = 0.10f), wd.everyday)
        OptionState.Wrong -> Triple(wd.accent, wd.accent.copy(alpha = 0.08f), wd.accent)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(fill)
            .border(1.5.dp, border, shape)
            .clickable(enabled = state == OptionState.Default, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(word.word, style = WordDropType.optionWord, color = text, modifier = Modifier.weight(1f))
        when (state) {
            OptionState.Correct -> Badge(WdIcons.Check, wd.everyday)
            OptionState.Wrong -> Badge(WdIcons.Close, wd.accent)
            else -> Unit
        }
    }
}

@Composable
private fun Badge(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Box(Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(color), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = wd.onAccent, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun Feedback(picked: WordEntity) {
    val shape = RoundedCornerShape(16.dp)
    val comesBack = stringResource(R.string.quiz_comes_back)
    Column(
        modifier = Modifier.fillMaxWidth().clip(shape).background(wd.surface).border(1.dp, wd.hairline, shape).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Eyebrow(stringResource(R.string.quiz_not_quite))
        Text(
            "${picked.word} means ${picked.definition.replaceFirstChar { it.lowercase() }.trimEnd('.')}. $comesBack",
            style = WordDropType.bodySmall,
            color = wd.muted,
        )
    }
}

@Composable
private fun Summary(s: QuizUiState.Finished, onOpenWord: (String) -> Unit, onSave: (String) -> Unit, onDone: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Title(progress = null)
        Column(modifier = Modifier.padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Eyebrow("Score")
            Text(
                stringResource(R.string.quiz_score, s.correctCount, s.total),
                style = WordDropType.headwordDetail,
                color = wd.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
        if (s.missed.isNotEmpty()) {
            Eyebrow(stringResource(R.string.quiz_missed), modifier = Modifier.padding(start = 20.dp, top = 40.dp, bottom = 10.dp))
            s.missed.forEach { w ->
                Hairline()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WordRow(w, onClick = { onOpenWord(w.id) }, showChevron = false, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.action_save),
                        style = WordDropType.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = wd.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { onSave(w.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
            Hairline()
        }
        Box(Modifier.padding(horizontal = 20.dp, vertical = 32.dp)) {
            PrimaryButton(stringResource(R.string.action_done), onDone, container = wd.ink, content = wd.paper, modifier = Modifier.fillMaxWidth())
        }
    }
}
