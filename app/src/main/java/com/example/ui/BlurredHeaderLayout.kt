package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.R

/**
 * 25% backdrop blur intensity (25% of a 40.dp maximum Gaussian blur radius = 10.dp),
 * allowing scrolled content moving behind the sticky header to remain visibly recognizable
 * like an iOS translucent navigation bar.
 */
private const val HEADER_BLUR_PERCENTAGE = 0.25f
private val MAX_HEADER_BLUR_RADIUS = 40.dp
val IOS_HEADER_BLUR_RADIUS: Dp = MAX_HEADER_BLUR_RADIUS * HEADER_BLUR_PERCENTAGE

/**
 * Computes a smooth, continuous collapse fraction in [0f, 1f] based on how far the user
 * has scrolled the [LazyListState], without mutating the list's top content padding.
 */
@Composable
fun rememberHeaderCollapseFraction(
    listState: LazyListState,
    collapseRangeDp: Dp = 108.dp
): State<Float> {
    val density = LocalDensity.current
    val collapseRangePx = with(density) { collapseRangeDp.toPx() }.coerceAtLeast(1f)
    val itemHeights = remember { mutableMapOf<Int, Int>() }

    return remember(listState, collapseRangePx) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            layoutInfo.visibleItemsInfo.forEach { itemInfo ->
                itemHeights[itemInfo.index] = itemInfo.size
            }
            val firstIndex = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset
            var totalScrolledPx = firstOffset
            for (i in 0 until firstIndex) {
                totalScrolledPx += itemHeights[i] ?: collapseRangePx.toInt()
                if (totalScrolledPx >= collapseRangePx) break
            }
            (totalScrolledPx.toFloat() / collapseRangePx).coerceIn(0f, 1f)
        }
    }
}

@Composable
fun BlurredStickyHeaderLayout(
    listState: LazyListState,
    scaffoldPadding: PaddingValues,
    modifier: Modifier = Modifier,
    defaultHeaderHeight: Dp = 166.dp,
    collapseRangeDp: Dp = 108.dp,
    headerContent: @Composable (collapseFraction: Float) -> Unit,
    scrollableContent: @Composable (topContentPadding: Dp) -> Unit
) {
    val contentGraphicsLayer = rememberGraphicsLayer()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val collapseFraction by rememberHeaderCollapseFraction(
        listState = listState,
        collapseRangeDp = collapseRangeDp
    )

    // Latch the expanded header height (when collapseFraction == 0f) so LazyColumn's top
    // contentPadding remains constant during scroll and never causes scroll offset jitter.
    var expandedHeaderHeightPx by remember { mutableIntStateOf(0) }
    val topStatusBarPadding = scaffoldPadding.calculateTopPadding()
    val bottomInsetPadding = scaffoldPadding.calculateBottomPadding()
    val startInsetPadding = scaffoldPadding.calculateStartPadding(layoutDirection)
    val endInsetPadding = scaffoldPadding.calculateEndPadding(layoutDirection)

    val topContentPaddingDp = if (expandedHeaderHeightPx > 0) {
        with(density) { expandedHeaderHeightPx.toDp() }
    } else {
        topStatusBarPadding + defaultHeaderHeight
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(
                start = startInsetPadding,
                end = endInsetPadding,
                bottom = bottomInsetPadding
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. Scrollable content layer recorded into contentGraphicsLayer on every scroll frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    @Suppress("UNUSED_VARIABLE")
                    val scrollIndex = listState.firstVisibleItemIndex
                    @Suppress("UNUSED_VARIABLE")
                    val scrollOffset = listState.firstVisibleItemScrollOffset

                    contentGraphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(contentGraphicsLayer)
                },
            contentAlignment = Alignment.TopCenter
        ) {
            scrollableContent(topContentPaddingDp)
        }

        // 2. Pinned iOS-style sticky header with 25% live background blur & smooth collapse
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    if (size.height > 0 && collapseFraction == 0f) {
                        expandedHeaderHeightPx = size.height
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // Blurred backdrop showing the live scrolling content underneath
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clipToBounds()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(
                            radius = IOS_HEADER_BLUR_RADIUS,
                            edgeTreatment = BlurredEdgeTreatment.Rectangle
                        )
                        .drawBehind {
                            @Suppress("UNUSED_VARIABLE")
                            val scrollIndex = listState.firstVisibleItemIndex
                            @Suppress("UNUSED_VARIABLE")
                            val scrollOffset = listState.firstVisibleItemScrollOffset

                            drawRect(backgroundColor)
                            drawLayer(contentGraphicsLayer)
                        }
                )

                // Translucent surface veil so content moving behind the header stays visible
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            backgroundColor.copy(
                                alpha = lerp(0.75f, 0.42f, collapseFraction)
                            )
                        )
                )
            }

            // Crisp foreground header content + subtle bottom hairline that fades in on scroll
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .padding(top = topStatusBarPadding)
                ) {
                    headerContent(collapseFraction)
                }

                if (collapseFraction > 0.02f) {
                    HorizontalDivider(
                        thickness = 0.6.dp,
                        color = outlineColor.copy(alpha = 0.55f * collapseFraction)
                    )
                }
            }
        }
    }
}

/**
 * Collapsing Home page header ("Rosaline."):
 * Starts at its original 78.dp top offset and 50.sp font size, and as the user scrolls up,
 * gradually shrinks until it reaches 30.sp (matching the 30.dp action buttons on the right)
 * and stays fixed there.
 */
@Composable
fun CollapsingHomeHeaderBar(
    collapseFraction: Float,
    onOpenTimeline: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topPadding = lerp(78.dp, 16.dp, collapseFraction)
    val bottomPadding = lerp(18.dp, 10.dp, collapseFraction)
    val titleFontSize = lerp(50.sp, 30.sp, collapseFraction)
    val titleLineHeight = lerp(54.sp, 34.sp, collapseFraction)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = topPadding,
                bottom = bottomPadding
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${stringResource(R.string.app_name)}.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = titleFontSize,
                lineHeight = titleLineHeight
            ),
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .testTag("greeting_heading")
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onOpenTimeline,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("open_timeline_button")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_rounded_view_timeline),
                    contentDescription = "Timeline",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(30.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("open_settings_button")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_rounded_settings),
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

/**
 * Collapsing Subpage header ("Timeline" / "Settings."):
 * Starts at 50.sp below the Back button. As the user scrolls up, the Back button scrolls up
 * and fades away while the title gradually shrinks to 30.sp and stays fixed at the top like
 * the Home page header. When the user scrolls back down, the Back button smoothly comes back down.
 */
@Composable
fun CollapsingSubpageHeaderBar(
    title: String,
    collapseFraction: Float,
    horizontalPadding: Dp,
    backButtonTestTag: String,
    headingTestTag: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleFontFamily: FontFamily? = null
) {
    val topPadding = lerp(60.dp, 16.dp, collapseFraction)
    val bottomPadding = lerp(16.dp, 10.dp, collapseFraction)
    val containerHeight = lerp(118.dp, 42.dp, collapseFraction)

    val titleFontSize = lerp(50.sp, 30.sp, collapseFraction)
    val titleLineHeight = lerp(54.sp, 34.sp, collapseFraction)

    // Back button scrolls upward off-screen as user scrolls up, and comes back down on scroll down
    val backButtonOffsetY = lerp(0.dp, (-64).dp, collapseFraction)
    val backButtonAlpha = (1f - collapseFraction * 1.4f).coerceIn(0f, 1f)

    // Title stays left-aligned (x = 0.dp) and rises smoothly into the sticky header bar
    val titleOffsetY = lerp(64.dp, 4.dp, collapseFraction)

    val baseTitleStyle = MaterialTheme.typography.headlineLarge
    val commonStyle = baseTitleStyle.copy(
        fontWeight = FontWeight.Black,
        fontSize = titleFontSize,
        lineHeight = titleLineHeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Proportional,
            trim = LineHeightStyle.Trim.None
        )
    )
    val effectiveStyle = if (titleFontFamily != null) {
        commonStyle.copy(fontFamily = titleFontFamily)
    } else {
        commonStyle
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = topPadding,
                bottom = bottomPadding
            )
            .height(containerHeight)
    ) {
        IconButton(
            onClick = onBackClick,
            enabled = backButtonAlpha > 0.05f,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(y = backButtonOffsetY)
                .graphicsLayer { alpha = backButtonAlpha }
                .size(48.dp)
                .testTag(backButtonTestTag)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_ms_rounded_arrow_back),
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = title,
            style = effectiveStyle,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(y = titleOffsetY)
                .testTag(headingTestTag)
        )
    }
}
