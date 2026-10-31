package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ProductEntry
import com.example.ui.theme.LibreFranklinFontFamily
import com.example.ui.theme.NewsreaderMaxBoldFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimelineEventType(val label: String) {
    START("Start"),
    END("End")
}

data class TimelineEventItem(
    val id: String,
    val productName: String,
    val productTag: String? = null,
    val timestampMillis: Long,
    val year: Int,
    val type: TimelineEventType
)

@Composable
fun TimelineScreen(
    allProducts: List<ProductEntry>,
    nowMillis: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackClick()
    }

    val eventsByYear = remember(allProducts, nowMillis) {
        buildTimelineEventsByYear(allProducts, nowMillis)
    }

    val currentYear = remember(nowMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.get(Calendar.YEAR)
    }

    val timelineListState = rememberLazyListState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("timeline_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BlurredStickyHeaderLayout(
            listState = timelineListState,
            scaffoldPadding = innerPadding,
            defaultHeaderHeight = 194.dp,
            headerContent = { collapseFraction ->
                CollapsingSubpageHeaderBar(
                    title = "Timeline",
                    collapseFraction = collapseFraction,
                    horizontalPadding = 24.dp,
                    backButtonTestTag = "timeline_back_button",
                    headingTestTag = "timeline_heading",
                    onBackClick = onBackClick,
                    titleFontFamily = NewsreaderMaxBoldFontFamily
                )
            }
        ) { topContentPadding ->
            LazyColumn(
                state = timelineListState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = topContentPadding + 12.dp,
                    bottom = 72.dp
                )
            ) {
                if (eventsByYear.isEmpty()) {
                    item(key = "empty_year_header") {
                        Text(
                            text = currentYear.toString(),
                            style = TextStyle(
                                fontFamily = LibreFranklinFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 34.sp,
                                lineHeight = 40.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 28.dp)
                        )
                    }
                    item(key = "empty_timeline_message") {
                        EmptyTimelineSegment()
                    }
                } else {
                    eventsByYear.forEach { (year, events) ->
                        item(key = "year_$year") {
                            Text(
                                text = year.toString(),
                                style = TextStyle(
                                    fontFamily = LibreFranklinFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 34.sp,
                                    lineHeight = 40.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(top = 4.dp, bottom = 24.dp)
                                    .testTag("timeline_year_$year")
                            )
                        }

                        // Top leading dashed line cap before the first event
                        item(key = "year_${year}_top_cap") {
                            TimelineDashedCap(heightDp = 36)
                        }

                        itemsIndexed(
                            items = events,
                            key = { _, item -> item.id }
                        ) { index, event ->
                            val isLastInYear = index == events.lastIndex
                            TimelineEventNode(
                                event = event,
                                bottomSegmentHeightDp = if (isLastInYear) 88 else 96
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineDashedCap(heightDp: Int) {
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
    ) {
        Spacer(modifier = Modifier.width(32.dp))
        Canvas(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
        ) {
            val centerX = size.width / 2f
            drawLine(
                color = lineColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = 3.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 9f), 0f)
            )
        }
    }
}

@Composable
private fun TimelineEventNode(
    event: TimelineEventItem,
    bottomSegmentHeightDp: Int
) {
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    val dotColor = MaterialTheme.colorScheme.onBackground
    val formattedDate = remember(event.timestampMillis) {
        formatOrdinalDayAndMonth(event.timestampMillis)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .testTag("timeline_event_${event.id}"),
        verticalAlignment = Alignment.Top
    ) {
        Spacer(modifier = Modifier.width(32.dp))

        // Vertical dashed line with black dot aligned to the date heading
        Canvas(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
        ) {
            val centerX = size.width / 2f
            val dotCenterY = 15.dp.toPx()
            val dotRadius = 4.8.dp.toPx()

            // Continuous vertical dashed line through the node
            drawLine(
                color = lineColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = 3.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 9f), 0f)
            )

            // Solid black circle dot on the timeline
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(centerX, dotCenterY)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Event Text Content: Date on top, Product Name + Outlined Start/End tag below
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = bottomSegmentHeightDp.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = formattedDate,
                style = TextStyle(
                    fontFamily = NewsreaderMaxBoldFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 27.sp,
                    lineHeight = 31.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (!event.productTag.isNullOrBlank()) {
                        "${event.productName} · ${event.productTag}"
                    } else {
                        event.productName
                    },
                    style = TextStyle(
                        fontFamily = LibreFranklinFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TimelineStatusDot(type = event.type)
            }
        }
    }
}

@Composable
private fun TimelineStatusDot(type: TimelineEventType) {
    val dotColor = when (type) {
        TimelineEventType.START -> Color(0xFF22C55E) // Green dot for Start
        TimelineEventType.END -> Color(0xFFEF4444)   // Red dot for End
    }

    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color = dotColor, shape = CircleShape)
            .testTag("timeline_status_dot_${type.name.lowercase()}")
    )
}

@Composable
private fun EmptyTimelineSegment() {
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    val dotColor = MaterialTheme.colorScheme.onBackground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        verticalAlignment = Alignment.Top
    ) {
        Spacer(modifier = Modifier.width(32.dp))

        Canvas(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
        ) {
            val centerX = size.width / 2f
            val dotY = 44.dp.toPx()
            drawLine(
                color = lineColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = 3.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 9f), 0f)
            )
            drawCircle(
                color = dotColor,
                radius = 4.8.dp.toPx(),
                center = Offset(centerX, dotY)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.padding(top = 30.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "No events yet",
                style = TextStyle(
                    fontFamily = NewsreaderMaxBoldFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    lineHeight = 30.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tap + on the home screen to start tracking a product.",
                style = TextStyle(
                    fontFamily = LibreFranklinFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildTimelineEventsByYear(
    products: List<ProductEntry>,
    nowMillis: Long
): Map<Int, List<TimelineEventItem>> {
    val allEvents = mutableListOf<TimelineEventItem>()
    val cal = Calendar.getInstance()

    products.forEach { product ->
        cal.timeInMillis = product.startTimeMillis
        val startYear = cal.get(Calendar.YEAR)
        allEvents.add(
            TimelineEventItem(
                id = "${product.id}_start",
                productName = product.name,
                productTag = product.tag,
                timestampMillis = product.startTimeMillis,
                year = startYear,
                type = TimelineEventType.START
            )
        )

        if (!product.isActive && product.endTimeMillis != null) {
            cal.timeInMillis = product.endTimeMillis
            val endYear = cal.get(Calendar.YEAR)
            allEvents.add(
                TimelineEventItem(
                    id = "${product.id}_end",
                    productName = product.name,
                    productTag = product.tag,
                    timestampMillis = product.endTimeMillis,
                    year = endYear,
                    type = TimelineEventType.END
                )
            )
        }
    }

    // Sort chronologically so Start appears first and End appears below it as in the Figma design
    val sorted = allEvents.sortedWith(
        compareBy<TimelineEventItem> { it.timestampMillis }
            .thenBy { if (it.type == TimelineEventType.START) 0 else 1 }
    )

    return sorted.groupBy { it.year }
}

private fun formatOrdinalDayAndMonth(timestampMillis: Long): String {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestampMillis
    }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val suffix = getDayOfMonthSuffix(day)
    val monthFormat = SimpleDateFormat("MMMM", Locale.ENGLISH)
    val monthName = monthFormat.format(Date(timestampMillis))
    return "$day$suffix, $monthName"
}

private fun getDayOfMonthSuffix(n: Int): String {
    if (n in 11..13) {
        return "th"
    }
    return when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}
