package com.soturine.replicascan.onboarding

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soturine.replicascan.R
import com.soturine.replicascan.core.ui.component.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val titles = listOf(R.string.onboarding_page_one_title, R.string.onboarding_page_two_title, R.string.onboarding_page_three_title)
    val bodies = listOf(R.string.onboarding_page_one_body, R.string.onboarding_page_two_body, R.string.onboarding_page_three_body)
    val scenes = listOf(ReplicaScanMascotState.Scanning, ReplicaScanMascotState.Working, ReplicaScanMascotState.Success)
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
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
        HorizontalPager(pager, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
                    ReplicaScanMascot(scenes[index], size = 220.dp, modifier = Modifier.padding(16.dp), reducedMotion = pager.currentPage != index)
                }
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
