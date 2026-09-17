package com.worddrop.app.ui.onboarding

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worddrop.app.R
import com.worddrop.app.data.prefs.UserPreferences
import com.worddrop.app.ui.components.Eyebrow
import com.worddrop.app.ui.components.PrimaryButton
import com.worddrop.app.ui.components.TextButtonPlain
import com.worddrop.app.ui.components.WdIcons
import com.worddrop.app.ui.theme.Newsreader
import com.worddrop.app.ui.theme.WordDropType
import com.worddrop.app.ui.theme.wd
import com.worddrop.app.widget.WidgetWord
import com.worddrop.app.widget.WordDropWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val prefs: UserPreferences) : ViewModel() {
    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.setOnboardingDone()
            onDone()
        }
    }
}

/** Requests the launcher's pin-widget sheet. Returns false when the launcher doesn't support it. */
fun requestPinWidget(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(ComponentName(context, WordDropWidgetReceiver::class.java), null, null)
}

@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(wd.paper)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 40.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Eyebrow(stringResource(R.string.app_name), color = wd.accent)

        WidgetPreview()

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                stringResource(R.string.onboarding_headline),
                style = WordDropType.screenTitle.copy(fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.015).em),
                color = wd.ink,
            )
            Text(stringResource(R.string.onboarding_body), style = WordDropType.definitionSmall, color = wd.muted)
        }

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(
                text = stringResource(R.string.action_add_widget),
                onClick = {
                    requestPinWidget(context)
                    vm.finish(onDone)
                },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButtonPlain(
                text = stringResource(R.string.action_maybe_later),
                onClick = { vm.finish(onDone) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** A faithful in-app rendering of the expanded widget so the user knows what they're adding. */
@Composable
private fun WidgetPreview(word: WidgetWord = WidgetWord.FALLBACK) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFD8CBB9), Color(0xFFC2B199))))
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xF5FFFDF9))
                .padding(start = 18.dp, top = 16.dp, end = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "WORDDROP · ${word.partOfSpeech.uppercase()}",
                    style = WordDropType.eyebrow.copy(fontSize = 10.sp),
                    color = Color(0xFF8A8177),
                    modifier = Modifier.weight(1f),
                )
                Icon(WdIcons.Refresh, contentDescription = null, tint = Color(0xFF6B645C), modifier = Modifier.height(18.dp))
            }
            Text(
                word.word,
                style = WordDropType.screenTitle.copy(fontFamily = Newsreader, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 34.sp),
                color = Color(0xFF1C1917),
            )
            word.phonetic?.let { Text(it, style = WordDropType.caption.copy(fontSize = 13.sp), color = Color(0xFF6B645C)) }
            Text(word.definition, style = WordDropType.caption.copy(fontSize = 14.sp), color = Color(0xFF1C1917), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(word.example, style = WordDropType.exampleSmall.copy(fontSize = 13.sp, lineHeight = 18.sp), color = Color(0xFF6B645C), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
