package com.soturine.replicascan.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.R
import com.soturine.replicascan.core.ui.component.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val titles = listOf(R.string.onboarding_page_one_title, R.string.onboarding_page_two_title, R.string.onboarding_page_three_title)
    val bodies = listOf(R.string.onboarding_page_one_body, R.string.onboarding_page_two_body, R.string.onboarding_page_three_body)
    val scenes = listOf(ReplicaScanMascotState.Welcome, ReplicaScanMascotState.Ocr, ReplicaScanMascotState.Success)
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val largeText = LocalDensity.current.fontScale >= 1.6f
    Column(modifier.fillMaxSize().safeDrawingPadding().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (pager.currentPage > 0) {
                IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.onboarding_back))
                }
            }
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onFinish) { Text(stringResource(R.string.onboarding_skip)) }
        }
        HorizontalPager(pager, modifier = Modifier.weight(1f).fillMaxWidth().testTag("onboarding-pager")) { index ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(top = if (largeText) 12.dp else 40.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (largeText) 14.dp else 20.dp)) {
                OnboardingPaperScene(scenes[index], largeText, pager.currentPage != index)
                Text(stringResource(titles[index]), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() })
                Text(stringResource(bodies[index]), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                Surface(Modifier.size(if (pager.currentPage == index) 24.dp else 8.dp, 8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = if (pager.currentPage == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant) {}
            }
        }
        ReplicaScanPrimaryButton(stringResource(if (pager.currentPage == 2) R.string.onboarding_finish else R.string.onboarding_next),
            onClick = { if (pager.currentPage == 2) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } })
    }
}

@Composable
private fun OnboardingPaperScene(state: ReplicaScanMascotState, compact: Boolean, reducedMotion: Boolean) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < .5f
    val paper = if (dark) colors.inverseSurface else colors.surface
    val ink = if (dark) colors.inverseOnSurface else colors.onSurface
    val size = if (compact) 170.dp else 242.dp
    val pageShape = RoundedCornerShape(9.dp)

    Box(Modifier.size(size)) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(colors.primaryContainer.copy(alpha = .48f),
                radius = this.size.width * .43f,
                center = Offset(this.size.width * .60f, this.size.height * .44f))
            drawLine(colors.outlineVariant,
                start = Offset(this.size.width * .10f, this.size.height * .91f),
                end = Offset(this.size.width * .90f, this.size.height * .91f),
                strokeWidth = 1.dp.toPx())
        }
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = 14.dp, y = (-9).dp)
                .size(width = size * .54f, height = size * .70f)
                .graphicsLayer { rotationZ = -8f }
                .shadow(9.dp, pageShape)
                .background(paper, pageShape)
                .border(1.dp, ink.copy(alpha = .14f), pageShape)
                .padding(16.dp),
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawRoundRect(colors.secondary.copy(alpha = .5f),
                    topLeft = Offset(0f, this.size.height * .07f),
                    size = Size(this.size.width * .46f, 5.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                listOf(.30f, .45f, .60f, .75f).forEach { y ->
                    drawLine(ink.copy(alpha = .18f),
                        start = Offset(0f, this.size.height * y),
                        end = Offset(this.size.width * .82f, this.size.height * y),
                        strokeWidth = 2.dp.toPx())
                }
            }
        }
        ReplicaScanMascot(state, size = size * .85f,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp),
            reducedMotion = reducedMotion)
    }
}
