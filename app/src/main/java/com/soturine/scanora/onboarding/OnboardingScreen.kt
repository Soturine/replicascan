package com.soturine.scanora.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soturine.scanora.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_page_one_title,
            bodyRes = R.string.onboarding_page_one_body,
            imageRes = R.drawable.onboarding_scanora_scan,
            imageDescriptionRes = R.string.onboarding_page_one_image_description,
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_page_two_title,
            bodyRes = R.string.onboarding_page_two_body,
            imageRes = R.drawable.onboarding_scanora_adjust,
            imageDescriptionRes = R.string.onboarding_page_two_image_description,
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_page_three_title,
            bodyRes = R.string.onboarding_page_three_body,
            imageRes = R.drawable.onboarding_scanora_privacy,
            imageDescriptionRes = R.string.onboarding_page_three_image_description,
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val page = pages[it]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = page.imageRes),
                        contentDescription = stringResource(id = page.imageDescriptionRes),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 410.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(page.titleRes), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(stringResource(page.bodyRes), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(pages.size) { index ->
                Surface(
                    modifier = Modifier.size(if (index == pagerState.currentPage) 22.dp else 10.dp, 10.dp),
                    shape = CircleShape,
                    color = if (index == pagerState.currentPage) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                ) {}
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            onClick = {
                if (pagerState.currentPage == pages.lastIndex) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
        ) {
            Icon(
                imageVector = if (pagerState.currentPage == pages.lastIndex) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = if (pagerState.currentPage == pages.lastIndex) {
                    stringResource(id = R.string.onboarding_finish)
                } else {
                    stringResource(id = R.string.onboarding_next)
                },
            )
        }
    }
}

private data class OnboardingPage(
    val titleRes: Int,
    val bodyRes: Int,
    val imageRes: Int,
    val imageDescriptionRes: Int,
)
