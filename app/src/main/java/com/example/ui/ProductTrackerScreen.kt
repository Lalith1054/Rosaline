package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ProductEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val QuickProductSuggestions = listOf(
    "Body Wash",
    "Shampoo",
    "Conditioner",
    "Toothpaste",
    "Face Cleanser",
    "Dish Soap",
    "Body Lotion",
    "Hand Soap"
)

@Composable
fun ProductTrackerScreen(
    viewModel: ProductTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Refresh timestamp whenever the user brings the app back to the foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (uiState.isTimelineOpen) {
        TimelineScreen(
            allProducts = uiState.allProducts,
            nowMillis = uiState.nowMillis,
            onBackClick = { viewModel.closeTimeline() },
            modifier = modifier
        )
        return
    }

    if (uiState.isSettingsOpen) {
        SettingsScreen(
            settings = uiState.userSettings,
            allProducts = uiState.allProducts,
            nowMillis = uiState.nowMillis,
            onBackClick = { viewModel.closeSettings() },
            onSelectTheme = { viewModel.setSelectedTheme(it) },
            onToggleDarkMode = { viewModel.setDarkModeEnabled(it) },
            onToggleCountDayOne = { viewModel.setCountStartDayAsDayOne(it) },
            onToggleLiveTimer = { viewModel.setShowLiveSubDayTimer(it) },
            onClearFinished = { viewModel.clearFinishedProducts() },
            onResetAllData = { viewModel.clearAllProducts() },
            modifier = modifier
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                modifier = Modifier
                    .size(76.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("add_product_fab")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_rounded_add),
                    contentDescription = stringResource(R.string.fab_add_product),
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .testTag("product_list"),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 36.dp,
                    bottom = 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item(key = "header") {
                    TrackerHeaderSection(
                        activeCount = uiState.activeCount,
                        finishedCount = uiState.finishedCount,
                        onOpenTimeline = { viewModel.openTimeline() },
                        onOpenSettings = { viewModel.openSettings() }
                    )
                }

                if (uiState.allProducts.isNotEmpty()) {
                    item(key = "filters") {
                        FilterPillsRow(
                            selectedFilter = uiState.selectedFilter,
                            totalCount = uiState.allProducts.size,
                            activeCount = uiState.activeCount,
                            finishedCount = uiState.finishedCount,
                            onSelectFilter = viewModel::setFilter
                        )
                    }
                }

                if (uiState.allProducts.isEmpty()) {
                    item(key = "empty_all") {
                        EmptyProductsCard()
                    }
                } else if (uiState.filteredProducts.isEmpty()) {
                    item(key = "empty_filter") {
                        EmptyFilterCard(
                            onResetFilter = { viewModel.setFilter(ProductFilter.ALL) }
                        )
                    }
                } else {
                    items(
                        items = uiState.filteredProducts,
                        key = { it.id }
                    ) { product ->
                        ProductItemCard(
                            product = product,
                            nowMillis = uiState.nowMillis,
                            countStartAsDayOne = uiState.userSettings.countStartDayAsDayOne,
                            showLiveTimer = uiState.userSettings.showLiveSubDayTimer,
                            onStopClick = {
                                viewModel.stopProduct(product) { stoppedItem, days ->
                                    val unit = if (days == 1L) "day" else "days"
                                    coroutineScope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(
                                                R.string.snackbar_stopped,
                                                stoppedItem.name,
                                                days,
                                                unit
                                            ),
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            onRestartClick = {
                                viewModel.restartProduct(product) { restartedName ->
                                    coroutineScope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(
                                                R.string.snackbar_restarted,
                                                restartedName
                                            ),
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            onDeleteClick = {
                                viewModel.deleteProduct(product) { deletedItem ->
                                    coroutineScope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = context.getString(
                                                R.string.snackbar_deleted,
                                                deletedItem.name
                                            ),
                                            actionLabel = context.getString(R.string.snackbar_undo),
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoDelete(deletedItem)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isAddDialogOpen) {
        AddProductDialog(
            onDismiss = { viewModel.closeAddDialog() },
            onConfirmAdd = { productName ->
                viewModel.addProduct(productName)
            }
        )
    }
}

@Composable
private fun TrackerHeaderSection(
    activeCount: Int,
    finishedCount: Int,
    onOpenTimeline: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 42.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.app_name)}.",
                style = MaterialTheme.typography.headlineLarge,
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

        // Summary Row without Avg Lifespan or divider lines
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("summary_banner"),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryStatColumn(
                value = activeCount.toString(),
                label = stringResource(R.string.summary_active_label),
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            SummaryStatColumn(
                value = finishedCount.toString(),
                label = stringResource(R.string.summary_finished_label),
                valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryStatColumn(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = valueColor,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilterPillsRow(
    selectedFilter: ProductFilter,
    totalCount: Int,
    activeCount: Int,
    finishedCount: Int,
    onSelectFilter: (ProductFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 6.dp)
            .testTag("filter_pills_row"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundedFilterChip(
            selected = selectedFilter == ProductFilter.ALL,
            label = "${stringResource(R.string.filter_all)} ($totalCount)",
            onClick = { onSelectFilter(ProductFilter.ALL) },
            testTag = "filter_all_chip"
        )
        RoundedFilterChip(
            selected = selectedFilter == ProductFilter.ACTIVE,
            label = "${stringResource(R.string.filter_active)} ($activeCount)",
            onClick = { onSelectFilter(ProductFilter.ACTIVE) },
            testTag = "filter_active_chip"
        )
        RoundedFilterChip(
            selected = selectedFilter == ProductFilter.FINISHED,
            label = "${stringResource(R.string.filter_finished)} ($finishedCount)",
            onClick = { onSelectFilter(ProductFilter.FINISHED) },
            testTag = "filter_finished_chip"
        )
    }
}

@Composable
private fun RoundedFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = Color.Transparent
        ),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    )
}

@Composable
private fun ProductItemCard(
    product: ProductEntry,
    nowMillis: Long,
    countStartAsDayOne: Boolean,
    showLiveTimer: Boolean,
    onStopClick: () -> Unit,
    onRestartClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val daysUsed = product.elapsedDaysCount(nowMillis, countStartAsDayOne)
    val liveDurationText = product.formattedLiveDuration(nowMillis)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Product Title, Status Badge & Days Used Counter Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadgePill(isActive = product.isActive)

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("product_name_${product.id}")
                    )

                    Text(
                        text = formatDateRangeText(
                            startMillis = product.startTimeMillis,
                            endMillis = product.endTimeMillis,
                            isActive = product.isActive
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Prominent Days Counter
                DaysUsedBadge(
                    days = daysUsed,
                    isActive = product.isActive,
                    productId = product.id
                )
            }

            // Bottom Row: Live elapsed timer info + Stop button (or Restart/Delete for finished items)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (product.isActive) {
                                R.drawable.ic_ms_rounded_hourglass_top
                            } else {
                                R.drawable.ic_ms_rounded_schedule
                            }
                        ),
                        contentDescription = null,
                        tint = if (product.isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when {
                            product.isActive && showLiveTimer -> "Active timer: $liveDurationText"
                            product.isActive -> "Timer running"
                            showLiveTimer -> "Lasted $daysUsed ${if (daysUsed == 1L) "day" else "days"} ($liveDurationText)"
                            else -> "Lasted $daysUsed ${if (daysUsed == 1L) "day" else "days"}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (product.isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_product_button_${product.id}")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ms_rounded_delete),
                            contentDescription = stringResource(R.string.btn_delete_product),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (product.isActive) {
                        Button(
                            onClick = onStopClick,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("stop_timer_button_${product.id}")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ms_rounded_stop),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_stop_timer),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onRestartClick,
                            shape = CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("restart_product_button_${product.id}")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ms_rounded_refresh),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_restart_product),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun StatusBadgePill(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        shape = CircleShape,
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ms_rounded_check_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = if (isActive) {
                    stringResource(R.string.status_in_use)
                } else {
                    stringResource(R.string.status_finished)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun DaysUsedBadge(
    days: Long,
    isActive: Boolean,
    productId: Long
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("days_counter_$productId"),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = days.toString(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = if (isActive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Text(
            text = if (days == 1L) "day used" else "days used",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyProductsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 40.dp)
            .testTag("empty_state_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Custom monochrome vector bottles & timer illustration
        MonochromeBottlesIllustration(
            modifier = Modifier.size(140.dp)
        )

        Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.empty_state_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyFilterCard(
    onResetFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.empty_filter_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.empty_filter_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        TextButton(
            onClick = onResetFilter,
            shape = CircleShape,
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Text(
                text = stringResource(R.string.filter_all),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun MonochromeBottlesIllustration(
    modifier: Modifier = Modifier
) {
    val blackColor = MaterialTheme.colorScheme.primary
    val lightGreyContainer = MaterialTheme.colorScheme.primaryContainer
    val midGrey = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft circular light-grey backdrop
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(lightGreyContainer, lightGreyContainer.copy(alpha = 0.1f))
            ),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.52f)
        )

        // Left tall pump bottle (Body wash)
        val bottle1Left = w * 0.24f
        val bottle1Top = h * 0.36f
        val bottle1Width = w * 0.24f
        val bottle1Height = h * 0.44f

        // Pump neck & nozzle
        drawRoundRect(
            color = blackColor,
            topLeft = Offset(bottle1Left + bottle1Width * 0.36f, bottle1Top - h * 0.11f),
            size = Size(bottle1Width * 0.28f, h * 0.12f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        val nozzlePath = Path().apply {
            moveTo(bottle1Left + bottle1Width * 0.35f, bottle1Top - h * 0.09f)
            lineTo(bottle1Left + bottle1Width * 0.85f, bottle1Top - h * 0.09f)
            lineTo(bottle1Left + bottle1Width * 0.92f, bottle1Top - h * 0.05f)
            lineTo(bottle1Left + bottle1Width * 0.35f, bottle1Top - h * 0.05f)
            close()
        }
        drawPath(nozzlePath, color = blackColor)

        // Pump bottle body
        drawRoundRect(
            color = blackColor,
            topLeft = Offset(bottle1Left, bottle1Top),
            size = Size(bottle1Width, bottle1Height),
            cornerRadius = CornerRadius(28f, 28f)
        )
        // Bottle label
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(bottle1Left + bottle1Width * 0.15f, bottle1Top + bottle1Height * 0.26f),
            size = Size(bottle1Width * 0.7f, bottle1Height * 0.46f),
            cornerRadius = CornerRadius(14f, 14f)
        )

        // Right shorter jar / bottle in medium grey
        val bottle2Left = w * 0.53f
        val bottle2Top = h * 0.46f
        val bottle2Width = w * 0.23f
        val bottle2Height = h * 0.34f

        drawRoundRect(
            color = midGrey.copy(alpha = 0.35f),
            topLeft = Offset(bottle2Left, bottle2Top),
            size = Size(bottle2Width, bottle2Height),
            cornerRadius = CornerRadius(26f, 26f)
        )
        drawRoundRect(
            color = blackColor,
            topLeft = Offset(bottle2Left + bottle2Width * 0.2f, bottle2Top - h * 0.06f),
            size = Size(bottle2Width * 0.6f, h * 0.065f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Floating monochrome timer badge on top right
        val badgeCenter = Offset(w * 0.68f, h * 0.30f)
        val badgeRadius = w * 0.13f
        drawCircle(
            color = Color.White,
            radius = badgeRadius,
            center = badgeCenter
        )
        drawCircle(
            color = blackColor,
            radius = badgeRadius,
            center = badgeCenter,
            style = Stroke(width = 6f)
        )
        // Clock hands inside badge
        drawLine(
            color = blackColor,
            start = badgeCenter,
            end = Offset(badgeCenter.x, badgeCenter.y - badgeRadius * 0.55f),
            strokeWidth = 5f
        )
        drawLine(
            color = blackColor,
            start = badgeCenter,
            end = Offset(badgeCenter.x + badgeRadius * 0.45f, badgeCenter.y),
            strokeWidth = 5f
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirmAdd: (String) -> Unit
) {
    var productName by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        runCatching { focusRequester.requestFocus() }
    }

    val submitProduct = {
        if (productName.trim().isEmpty()) {
            showError = true
        } else {
            onConfirmAdd(productName.trim())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.dialog_add_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.dialog_add_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = {
                        productName = it
                        if (showError && it.trim().isNotEmpty()) {
                            showError = false
                        }
                    },
                    label = {
                        Text(text = stringResource(R.string.input_product_name_label))
                    },
                    placeholder = {
                        Text(text = stringResource(R.string.input_product_name_placeholder))
                    },
                    isError = showError,
                    supportingText = if (showError) {
                        {
                            Text(
                                text = stringResource(R.string.error_empty_name),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitProduct() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("product_name_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.quick_pick_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        QuickProductSuggestions.forEach { suggestion ->
                            SuggestionChip(
                                onClick = {
                                    productName = suggestion
                                    showError = false
                                },
                                shape = CircleShape,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = null,
                                label = {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                modifier = Modifier.testTag("suggestion_chip_${suggestion.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = submitProduct,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_start_timer_button")
            ) {
                Text(
                    text = stringResource(R.string.btn_start_timer),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cancel_add_product_button")
            ) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

private fun formatDateRangeText(
    startMillis: Long,
    endMillis: Long?,
    isActive: Boolean
): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val startStr = formatter.format(Date(startMillis))
    return if (isActive || endMillis == null) {
        "Started $startStr"
    } else {
        val endStr = formatter.format(Date(endMillis))
        if (startStr == endStr) {
            "Used on $startStr"
        } else {
            "$startStr – $endStr"
        }
    }
}
