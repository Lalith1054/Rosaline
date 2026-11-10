package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ProductEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class ActiveScreenPage {
    HOME,
    SETTINGS
}

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

    val activePage = when {
        uiState.isSettingsOpen -> ActiveScreenPage.SETTINGS
        else -> ActiveScreenPage.HOME
    }

    AnimatedContent(
        targetState = activePage,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        label = "screen_page_transition"
    ) { page ->
        when (page) {
            ActiveScreenPage.SETTINGS -> {
                SettingsScreen(
                    settings = uiState.userSettings,
                    allProducts = uiState.allProducts,
                    nowMillis = uiState.nowMillis,
                    onBackClick = { viewModel.closeSettings() },
                    onSelectTheme = { viewModel.setSelectedTheme(it) },
                    onToggleDarkMode = { viewModel.setDarkModeEnabled(it) },
                    onToggleCountDayOne = { viewModel.setCountStartDayAsDayOne(it) },
                    onClearFinished = { viewModel.clearFinishedProducts() },
                    onResetAllData = { viewModel.clearAllProducts() },
                    onUpdateGithubRepo = { viewModel.setGithubRepo(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            ActiveScreenPage.HOME -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
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
                    val listState = rememberLazyListState()
                    val density = LocalDensity.current
                    val expandRangeDp = 60.dp
                    val expandRangePx = with(density) { expandRangeDp.toPx() }
                    val collapseFraction by remember(expandRangePx) {
                        derivedStateOf {
                            if (listState.firstVisibleItemIndex > 0) {
                                1f
                            } else {
                                (listState.firstVisibleItemScrollOffset.toFloat() / expandRangePx.coerceAtLeast(1f))
                                    .coerceIn(0f, 1f)
                            }
                        }
                    }
                    val isScrolled by remember {
                        derivedStateOf {
                            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 4
                        }
                    }
                    val contentGraphicsLayer = rememberGraphicsLayer()
                    val headerBlurLayer = rememberGraphicsLayer()
                    val bgColor = MaterialTheme.colorScheme.background

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 640.dp)
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawWithContent {
                                        contentGraphicsLayer.record {
                                            drawRect(bgColor)
                                            this@drawWithContent.drawContent()
                                        }
                                        drawLayer(contentGraphicsLayer)
                                    }
                                    .testTag("product_list"),
                                contentPadding = PaddingValues(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = innerPadding.calculateTopPadding() + 88.dp,
                                    bottom = 112.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                item(key = "header_expand_spacer") {
                                    Spacer(modifier = Modifier.height(expandRangeDp))
                                }

                                item(key = "summary") {
                                    TrackerSummaryBanner(
                                        activeCount = uiState.activeCount,
                                        finishedCount = uiState.finishedCount
                                    )
                                }

                            if (uiState.allProducts.isNotEmpty()) {
                                item(key = "filters") {
                                    FilterPillsRow(
                                        selectedFilter = uiState.selectedFilter,
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

                            // Blurred sticky header overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(bgColor)
                                        .clipToBounds()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(
                                                radius = 6.dp,
                                                edgeTreatment = BlurredEdgeTreatment.Rectangle
                                            )
                                            .drawBehind {
                                                val w = size.width.toInt()
                                                val h = size.height.toInt()
                                                if (w > 0 && h > 0) {
                                                    headerBlurLayer.record(size = IntSize(w, h)) {
                                                        drawRect(bgColor)
                                                        drawLayer(contentGraphicsLayer)
                                                    }
                                                    drawLayer(headerBlurLayer)
                                                }
                                            }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    0.0f to bgColor.copy(alpha = 0.34f),
                                                    0.75f to bgColor.copy(alpha = 0.28f),
                                                    1.0f to bgColor.copy(alpha = 0.22f)
                                                )
                                            )
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = innerPadding.calculateTopPadding() + lerp(64.dp, 20.dp, collapseFraction),
                                            bottom = lerp(14.dp, 10.dp, collapseFraction),
                                            start = 20.dp,
                                            end = 20.dp
                                        )
                                ) {
                                    TrackerGreetingBar(
                                        collapseFraction = collapseFraction,
                                        onOpenSettings = { viewModel.openSettings() }
                                    )
                                }

                                if (isScrolled) {
                                    HorizontalDivider(
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        thickness = 0.6.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddDialogOpen) {
        AddProductDialog(
            availableTags = uiState.userSettings.allAvailableTags,
            onCreateCustomTag = { newTag ->
                viewModel.addCustomTag(newTag)
            },
            onDismiss = { viewModel.closeAddDialog() },
            onConfirmAdd = { productName, selectedTag, imageUri, startTimeMillis ->
                viewModel.addProduct(productName, selectedTag, imageUri, startTimeMillis)
            }
        )
    }
}


@Composable
private fun TrackerGreetingBar(
    collapseFraction: Float,
    onOpenSettings: () -> Unit
) {
    val headingFontSize = lerp(48.sp, 28.sp, collapseFraction)
    val headingLineHeight = lerp(52.sp, 32.sp, collapseFraction)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${stringResource(R.string.app_name)}.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = headingFontSize,
                lineHeight = headingLineHeight
            ),
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .testTag("greeting_heading")
        )

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

@Composable
private fun TrackerSummaryBanner(
    activeCount: Int,
    finishedCount: Int
) {
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
    onSelectFilter: (ProductFilter) -> Unit
) {
    val currentOnSelectFilter by rememberUpdatedState(onSelectFilter)
    val currentSelectedFilter by rememberUpdatedState(selectedFilter)
    val segmentBounds = remember { mutableStateMapOf<ProductFilter, ClosedFloatingPointRange<Float>>() }

    fun resolveFilterAtX(x: Float): ProductFilter? {
        for ((filter, range) in segmentBounds) {
            if (x in range) return filter
        }
        val sorted = segmentBounds.entries.sortedBy { it.value.start }
        if (sorted.isEmpty()) return null
        if (x < sorted.first().value.start) return sorted.first().key
        if (x > sorted.last().value.endInclusive) return sorted.last().key
        return sorted.minByOrNull { entry ->
            val center = (entry.value.start + entry.value.endInclusive) / 2f
            kotlin.math.abs(x - center)
        }?.key
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 10.dp)
            .testTag("filter_pills_row"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 5.dp, vertical = 4.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            resolveFilterAtX(down.position.x)?.let { target ->
                                if (target != currentSelectedFilter) {
                                    currentOnSelectFilter(target)
                                }
                            }
                            do {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull()
                                if (pointer != null && pointer.pressed) {
                                    resolveFilterAtX(pointer.position.x)?.let { target ->
                                        if (target != currentSelectedFilter) {
                                            currentOnSelectFilter(target)
                                            pointer.consume()
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SegmentedFilterItem(
                    selected = selectedFilter == ProductFilter.ALL,
                    label = stringResource(R.string.filter_all),
                    onClick = { onSelectFilter(ProductFilter.ALL) },
                    testTag = "filter_all_chip",
                    onBoundsUpdated = { range -> segmentBounds[ProductFilter.ALL] = range }
                )
                SegmentedFilterItem(
                    selected = selectedFilter == ProductFilter.ACTIVE,
                    label = stringResource(R.string.filter_active),
                    onClick = { onSelectFilter(ProductFilter.ACTIVE) },
                    testTag = "filter_active_chip",
                    onBoundsUpdated = { range -> segmentBounds[ProductFilter.ACTIVE] = range }
                )
                SegmentedFilterItem(
                    selected = selectedFilter == ProductFilter.FINISHED,
                    label = stringResource(R.string.filter_finished),
                    onClick = { onSelectFilter(ProductFilter.FINISHED) },
                    testTag = "filter_finished_chip",
                    onBoundsUpdated = { range -> segmentBounds[ProductFilter.FINISHED] = range }
                )
            }
        }
    }
}

@Composable
private fun SegmentedFilterItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    onBoundsUpdated: (ClosedFloatingPointRange<Float>) -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "filterBgColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "filterContentColor"
    )

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                val startX = coords.positionInParent().x
                val endX = startX + coords.size.width
                onBoundsUpdated(startX..endX)
            }
            .clip(RoundedCornerShape(7.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ),
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
private fun ProductItemCard(
    product: ProductEntry,
    nowMillis: Long,
    countStartAsDayOne: Boolean,
    onStopClick: () -> Unit,
    onRestartClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val daysUsed = product.elapsedDaysCount(nowMillis, countStartAsDayOne)
    val productBitmap = remember(product.imageUri) {
        product.imageUri?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching {
                val file = File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        if (productBitmap != null) {
            val imageSquareSize = 96.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: 1:1 Square product photo with top-right circular tag badge
                Box(
                    modifier = Modifier
                        .size(imageSquareSize)
                        .testTag("product_image_container_${product.id}")
                ) {
                    Image(
                        bitmap = productBitmap,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 0.8.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    )

                    if (!product.tag.isNullOrBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            border = BorderStroke(1.8.dp, MaterialTheme.colorScheme.background),
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 5.dp, y = (-5).dp)
                                .testTag("product_tag_icon_${product.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    painter = painterResource(id = resolveTagIconRes(product.tag)),
                                    contentDescription = product.tag,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Right Side: Matches the square image height, bringing the buttons up flush with the bottom of the square
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(imageSquareSize),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .alignByBaseline()
                                        .testTag("product_name_${product.id}")
                                )

                                if (!product.tag.isNullOrBlank()) {
                                    ProductTagBadge(
                                        tag = product.tag,
                                        modifier = Modifier
                                            .alignByBaseline()
                                            .testTag("product_tag_${product.id}")
                                    )
                                }
                            }

                            Text(
                                text = formatDateRangeText(
                                    startMillis = product.startTimeMillis,
                                    endMillis = product.endTimeMillis,
                                    isActive = product.isActive
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        DaysUsedBadge(
                            days = daysUsed,
                            isActive = product.isActive,
                            productId = product.id,
                            topPadding = 0.dp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("delete_product_button_${product.id}")
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ms_rounded_delete),
                                    contentDescription = stringResource(R.string.btn_delete_product),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            if (product.isActive) {
                                Button(
                                    onClick = onStopClick,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("stop_timer_button_${product.id}")
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ms_rounded_stop),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
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
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("restart_product_button_${product.id}")
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ms_rounded_refresh),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
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
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Row: Tag Icon + Name + Category Tag + Date on Left, Days Used Hero Counter on Right
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
                        if (!product.tag.isNullOrBlank()) {
                            Icon(
                                painter = painterResource(id = resolveTagIconRes(product.tag)),
                                contentDescription = product.tag,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("product_tag_icon_${product.id}")
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .alignByBaseline()
                                    .testTag("product_name_${product.id}")
                            )

                            if (!product.tag.isNullOrBlank()) {
                                ProductTagBadge(
                                    tag = product.tag,
                                    modifier = Modifier
                                        .alignByBaseline()
                                        .testTag("product_tag_${product.id}")
                                )
                            }
                        }

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

                    DaysUsedBadge(
                        days = daysUsed,
                        isActive = product.isActive,
                        productId = product.id,
                        topPadding = 12.dp
                    )
                }

                // Bottom Row: Action Buttons on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("delete_product_button_${product.id}")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ms_rounded_delete),
                                contentDescription = stringResource(R.string.btn_delete_product),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        if (product.isActive) {
                            Button(
                                onClick = onStopClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("stop_timer_button_${product.id}")
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ms_rounded_stop),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
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
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("restart_product_button_${product.id}")
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ms_rounded_refresh),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
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
        }

        HorizontalDivider(
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun ProductTagBadge(
    tag: String,
    modifier: Modifier = Modifier
) {
    val capitalCaseTag = remember(tag) {
        tag.trim().split(Regex("\\s+")).joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }
    Text(
        text = capitalCaseTag,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

private fun resolveTagIconRes(tag: String): Int {
    val normalized = tag.trim().lowercase(Locale.ROOT)
    return when {
        normalized == "bodywash" || normalized.contains("body wash") || normalized.contains("shower") ->
            R.drawable.ic_tag_bodywash
        normalized == "shampoo" || normalized.contains("shampoo") || normalized.contains("hair wash") ->
            R.drawable.ic_tag_shampoo
        normalized == "conditioner" || normalized.contains("conditioner") || normalized.contains("serum") ->
            R.drawable.ic_tag_conditioner
        normalized == "toothpaste" || normalized.contains("tooth") || normalized.contains("mouth") || normalized.contains("dental") ->
            R.drawable.ic_tag_toothpaste
        normalized == "face cleanser" || normalized.contains("face") || normalized.contains("cleanser") || normalized.contains("toner") ->
            R.drawable.ic_tag_face_cleanser
        normalized == "body lotion" || normalized.contains("lotion") || normalized.contains("cream") || normalized.contains("moisturizer") || normalized.contains("sunscreen") ->
            R.drawable.ic_tag_body_lotion
        normalized == "hand soap" || normalized.contains("hand") ->
            R.drawable.ic_tag_hand_soap
        normalized == "dish soap" || normalized.contains("dish") || normalized.contains("detergent") ->
            R.drawable.ic_tag_dish_soap
        else -> R.drawable.ic_tag_custom
    }
}



@Composable
private fun DaysUsedBadge(
    days: Long,
    isActive: Boolean,
    productId: Long,
    topPadding: androidx.compose.ui.unit.Dp = 18.dp
) {
    val unitLabel = if (days == 1L) "day" else "days"

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(top = topPadding)
    ) {
        Text(
            text = days.toString(),
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                lineHeight = 36.sp
            ),
            color = if (isActive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.testTag("days_counter_${productId}")
        )
        Text(
            text = unitLabel.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall,
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
            shape = RoundedCornerShape(10.dp),
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddProductDialog(
    availableTags: List<String>,
    onCreateCustomTag: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirmAdd: (String, String?, String?, Long?) -> Unit
) {
    val context = LocalContext.current
    var productName by rememberSaveable { mutableStateOf("") }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedImagePath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedStartDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var isDatePickerOpen by rememberSaveable { mutableStateOf(false) }
    var showError by rememberSaveable { mutableStateOf(false) }
    var isCreatingCustomTag by rememberSaveable { mutableStateOf(false) }
    var customTagInput by rememberSaveable { mutableStateOf("") }

    val formattedSelectedDate = remember(selectedStartDateMillis) {
        selectedStartDateMillis?.let { millis ->
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            val savedPath = savePickedImageToInternalStorage(context, pickedUri)
            if (savedPath != null) {
                selectedImagePath = savedPath
            }
        }
    }

    val previewBitmap = remember(selectedImagePath) {
        selectedImagePath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching {
                val file = File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else {
                    null
                }
            }.getOrNull()
        }
    }

    val submitCustomTag = {
        val cleaned = customTagInput.trim()
        if (cleaned.isNotEmpty()) {
            onCreateCustomTag(cleaned)
            selectedTag = cleaned
            customTagInput = ""
            isCreatingCustomTag = false
        }
    }

    val submitProduct = {
        val finalName = productName.trim()
        if (finalName.isEmpty()) {
            showError = true
        } else {
            onConfirmAdd(finalName, selectedTag, selectedImagePath, selectedStartDateMillis)
        }
    }

    if (isDatePickerOpen) {
        val todayEndUtcMillis = remember {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }
        val initialUtcMillis = remember(selectedStartDateMillis) {
            val localCal = Calendar.getInstance().apply {
                timeInMillis = selectedStartDateMillis ?: System.currentTimeMillis()
            }
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH),
                    12,
                    0,
                    0
                )
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayEndUtcMillis
                }

                override fun isSelectableYear(year: Int): Boolean {
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    return year <= currentYear
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { isDatePickerOpen = false },
            shape = RoundedCornerShape(12.dp),
            confirmButton = {
                Button(
                    onClick = {
                        val pickedUtc = datePickerState.selectedDateMillis
                        if (pickedUtc != null) {
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = pickedUtc
                            }
                            val todayLocal = Calendar.getInstance()
                            val isTodayPicked =
                                utcCal.get(Calendar.YEAR) == todayLocal.get(Calendar.YEAR) &&
                                    utcCal.get(Calendar.MONTH) == todayLocal.get(Calendar.MONTH) &&
                                    utcCal.get(Calendar.DAY_OF_MONTH) == todayLocal.get(Calendar.DAY_OF_MONTH)

                            selectedStartDateMillis = if (isTodayPicked) {
                                null
                            } else {
                                Calendar.getInstance().apply {
                                    set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                    set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                    set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }
                        }
                        isDatePickerOpen = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("confirm_start_date_button")
                ) {
                    Text(
                        text = "Select",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { isDatePickerOpen = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_cancel),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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
                        .testTag("product_name_input")
                )

                // Start Date Selector (Today or Previous Date)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Start date",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isTodaySelected = selectedStartDateMillis == null

                        Surface(
                            onClick = { selectedStartDateMillis = null },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isTodaySelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isTodaySelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("start_date_today_button")
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isTodaySelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }

                        Surface(
                            onClick = { isDatePickerOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isTodaySelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (!isTodaySelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("start_date_picker_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ms_rounded_calendar_today),
                                    contentDescription = null,
                                    tint = if (!isTodaySelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = formattedSelectedDate ?: "Previous Date",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (!isTodaySelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }

                // Optional Product Photo Picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Product photo (optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (previewBitmap != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp, end = 4.dp)
                                    .size(56.dp)
                            ) {
                                Image(
                                    bitmap = previewBitmap,
                                    contentDescription = "Selected product photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = 0.8.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                )
                                val currentTag = selectedTag
                                if (!currentTag.isNullOrBlank()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .size(22.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-6).dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                painter = painterResource(id = resolveTagIconRes(currentTag)),
                                                contentDescription = currentTag,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("change_product_photo_button")
                                ) {
                                    Text(
                                        text = "Change",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }

                                TextButton(
                                    onClick = { selectedImagePath = null },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("remove_product_photo_button")
                                ) {
                                    Text(
                                        text = "Remove",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pick_product_photo_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ms_rounded_add),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Add Photo (Optional)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.quick_pick_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableTags.forEach { tag ->
                            val isSelected = selectedTag?.equals(tag, ignoreCase = true) == true

                            SuggestionChip(
                                onClick = {
                                    selectedTag = if (isSelected) null else tag
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    labelColor = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    iconContentColor = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                ),
                                border = null,
                                icon = {
                                    Icon(
                                        painter = painterResource(id = resolveTagIconRes(tag)),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                modifier = Modifier.testTag("tag_chip_${tag.lowercase().replace(" ", "_")}")
                            )
                        }
                    }

                    // Bottom option to create a custom tag
                    if (isCreatingCustomTag) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customTagInput,
                                onValueChange = { customTagInput = it },
                                label = {
                                    Text(text = stringResource(R.string.input_custom_tag_label))
                                },
                                placeholder = {
                                    Text(text = stringResource(R.string.input_custom_tag_placeholder))
                                },
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
                                    onDone = { submitCustomTag() }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_tag_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        isCreatingCustomTag = false
                                        customTagInput = ""
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text(
                                        text = stringResource(R.string.btn_cancel),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                FilledTonalButton(
                                    onClick = submitCustomTag,
                                    enabled = customTagInput.trim().isNotEmpty(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("confirm_custom_tag_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.btn_add_custom_tag),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            onClick = { isCreatingCustomTag = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("create_custom_tag_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_create_custom_tag),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = submitProduct,
                shape = RoundedCornerShape(10.dp),
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
                shape = RoundedCornerShape(10.dp),
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

private fun savePickedImageToInternalStorage(context: Context, uri: Uri): String? {
    return runCatching {
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        val maxSide = 512
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scaledBitmap = if (width > maxSide || height > maxSide) {
            val ratio = width.toFloat() / height.toFloat()
            val targetW = if (ratio >= 1f) maxSide else (maxSide * ratio).toInt().coerceAtLeast(1)
            val targetH = if (ratio >= 1f) (maxSide / ratio).toInt().coerceAtLeast(1) else maxSide
            Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
        } else {
            originalBitmap
        }

        val dir = File(context.filesDir, "product_images").apply {
            if (!exists()) mkdirs()
        }
        val destFile = File(dir, "product_${System.currentTimeMillis()}.jpg")
        FileOutputStream(destFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        destFile.absolutePath
    }.getOrNull()
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
