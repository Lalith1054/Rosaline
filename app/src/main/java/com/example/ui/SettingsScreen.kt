package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppThemeOption
import com.example.data.ProductEntry
import com.example.data.UserSettings
import com.example.ui.theme.ChocolateBackground
import com.example.ui.theme.MonoBackground
import com.example.ui.theme.NewsreaderFontFamily
import com.example.ui.theme.OmeletteBackground
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: UserSettings,
    allProducts: List<ProductEntry>,
    nowMillis: Long,
    onBackClick: () -> Unit,
    onSelectTheme: (AppThemeOption) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleCountDayOne: (Boolean) -> Unit,
    onToggleLiveTimer: (Boolean) -> Unit,
    onClearFinished: () -> Unit,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showClearFinishedDialog by rememberSaveable { mutableStateOf(false) }
    var showResetAllDialog by rememberSaveable { mutableStateOf(false) }

    val finishedCount = allProducts.count { !it.isActive }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 36.dp,
                    bottom = 64.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Header Row with Back Icon & Settings. heading
                item(key = "settings_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_back_button")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ms_rounded_arrow_back),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "Settings.",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.testTag("settings_heading")
                        )
                    }
                }

                // Section: Themes (Dark Mode switch + Classic White, A Certain Chocolate Milkshake, The Usual Bread Omelette)
                item(key = "section_themes") {
                    SettingsSectionTitle(title = "THEMES")
                    SettingsToggleRow(
                        iconRes = R.drawable.ic_ms_rounded_dark_mode,
                        title = "Dark Mode",
                        subtitle = "Switch to a sleek dark surface palette",
                        checked = settings.darkModeEnabled,
                        onCheckedChange = onToggleDarkMode,
                        testTag = "toggle_dark_mode"
                    )
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    ThemeSelectorRow(
                        selectedTheme = settings.selectedTheme,
                        darkModeEnabled = settings.darkModeEnabled,
                        onSelectTheme = onSelectTheme
                    )
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Section: Display & Timer Preferences
                item(key = "section_preferences") {
                    SettingsSectionTitle(
                        title = "TIMER & DISPLAY",
                        modifier = Modifier.padding(top = 28.dp)
                    )
                }

                item(key = "pref_day_one") {
                    SettingsToggleRow(
                        iconRes = R.drawable.ic_ms_rounded_calendar_today,
                        title = "Count start day as Day 1",
                        subtitle = "New products start at 1 day used instead of 0 days on the first day",
                        checked = settings.countStartDayAsDayOne,
                        onCheckedChange = onToggleCountDayOne,
                        testTag = "toggle_count_day_one"
                    )
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                item(key = "pref_live_timer") {
                    SettingsToggleRow(
                        iconRes = R.drawable.ic_ms_rounded_timer,
                        title = "Show live elapsed timer",
                        subtitle = "Display real-time hours, minutes, and seconds on active product items",
                        checked = settings.showLiveSubDayTimer,
                        onCheckedChange = onToggleLiveTimer,
                        testTag = "toggle_live_timer"
                    )
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Section: Data & History Management
                item(key = "section_data") {
                    SettingsSectionTitle(
                        title = "DATA & HISTORY",
                        modifier = Modifier.padding(top = 28.dp)
                    )
                }

                item(key = "action_export") {
                    SettingsActionRow(
                        iconRes = R.drawable.ic_ms_rounded_ios_share,
                        title = "Export product log",
                        subtitle = if (allProducts.isEmpty()) {
                            "No tracked products to export yet"
                        } else {
                            "Share a summary of all ${allProducts.size} tracked products"
                        },
                        buttonLabel = "Export",
                        enabled = allProducts.isNotEmpty(),
                        onClick = {
                            val exportText = buildExportSummary(
                                products = allProducts,
                                nowMillis = nowMillis,
                                countStartAsDayOne = settings.countStartDayAsDayOne
                            )
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Rosaline Product Lifespan Log")
                                putExtra(Intent.EXTRA_TEXT, exportText)
                            }
                            val chooser = Intent.createChooser(sendIntent, "Export Product Log")
                            runCatching { context.startActivity(chooser) }
                        },
                        testTag = "export_log_button"
                    )
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                item(key = "action_clear_finished") {
                    SettingsActionRow(
                        iconRes = R.drawable.ic_ms_rounded_delete_sweep,
                        title = "Clear finished products",
                        subtitle = if (finishedCount == 0) {
                            "No finished items in your list"
                        } else {
                            "Remove $finishedCount finished ${if (finishedCount == 1) "item" else "items"} while keeping active timers"
                        },
                        buttonLabel = "Clear",
                        enabled = finishedCount > 0,
                        onClick = { showClearFinishedDialog = true },
                        testTag = "clear_finished_button"
                    )
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                item(key = "action_reset_all") {
                    SettingsActionRow(
                        iconRes = R.drawable.ic_ms_rounded_delete,
                        title = "Reset all product data",
                        subtitle = if (allProducts.isEmpty()) {
                            "Database is currently empty"
                        } else {
                            "Permanently delete all ${allProducts.size} active and finished items"
                        },
                        buttonLabel = "Reset",
                        enabled = allProducts.isNotEmpty(),
                        isDestructive = true,
                        onClick = { showResetAllDialog = true },
                        testTag = "reset_all_data_button"
                    )
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Section: About
                item(key = "section_about") {
                    SettingsSectionTitle(
                        title = "ABOUT",
                        modifier = Modifier.padding(top = 28.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_ms_rounded_info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Rosaline · Version 1.0",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "All product timers and logs are stored locally on your device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Dedication Note at the bottom of the Settings page
                item(key = "dedication_note") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 36.dp, bottom = 20.dp, start = 12.dp, end = 12.dp)
                            .testTag("dedication_note"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "I created this little app especially for you, Ammu. It's my way of putting a little thought and care into the everyday things you do. Thank you for being a part of my life and making it all the more beautiful. I hope this little creation reminds you how much your presence means to me.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = NewsreaderFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 19.sp,
                                lineHeight = 28.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "With love,\nAbi",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = NewsreaderFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                lineHeight = 27.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showClearFinishedDialog) {
        ConfirmSettingsDialog(
            title = "Clear finished products?",
            body = "This will remove all $finishedCount finished products from your list. Active timers will continue running.",
            confirmText = "Clear Finished",
            onDismiss = { showClearFinishedDialog = false },
            onConfirm = {
                showClearFinishedDialog = false
                onClearFinished()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Cleared finished products",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }

    if (showResetAllDialog) {
        ConfirmSettingsDialog(
            title = "Reset all product data?",
            body = "This will permanently delete all active and finished products from your device.",
            confirmText = "Delete Everything",
            onDismiss = { showResetAllDialog = false },
            onConfirm = {
                showResetAllDialog = false
                onResetAllData()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "All product logs have been reset",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSelectorRow(
    selectedTheme: AppThemeOption,
    darkModeEnabled: Boolean,
    onSelectTheme: (AppThemeOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_ms_rounded_palette),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Background Atmosphere",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose your preferred light theme tone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppThemeOption.entries.forEach { option ->
                val isSelected = !darkModeEnabled && option == selectedTheme
                val swatchColor = when (option) {
                    AppThemeOption.CLASSIC_WHITE -> MonoBackground
                    AppThemeOption.CHOCOLATE_MILKSHAKE -> ChocolateBackground
                    AppThemeOption.BREAD_OMELETTE -> OmeletteBackground
                }

                Surface(
                    onClick = { onSelectTheme(option) },
                    shape = CircleShape,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("theme_option_${option.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(color = swatchColor, shape = CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) {
                                        Color.White.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SettingsActionRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    buttonLabel: String,
    enabled: Boolean,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isDestructive) {
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(testTag)
            ) {
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        } else {
            FilledTonalButton(
                onClick = onClick,
                enabled = enabled,
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(testTag)
            ) {
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ConfirmSettingsDialog(
    title: String,
    body: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_settings_dialog_button")
            ) {
                Text(
                    text = confirmText,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}

private fun buildExportSummary(
    products: List<ProductEntry>,
    nowMillis: Long,
    countStartAsDayOne: Boolean
): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val sb = StringBuilder()
    sb.appendLine("Rosaline. — Product Lifespan Log")
    sb.appendLine("--------------------------------")
    products.forEach { item ->
        val days = item.elapsedDaysCount(nowMillis, countStartAsDayOne)
        val startStr = formatter.format(Date(item.startTimeMillis))
        val statusStr = if (item.isActive) {
            "In Use (Started $startStr)"
        } else {
            val endStr = formatter.format(Date(item.endTimeMillis ?: nowMillis))
            "Finished ($startStr – $endStr)"
        }
        sb.appendLine("• ${item.name}: $days ${if (days == 1L) "day" else "days"} [$statusStr]")
    }
    return sb.toString()
}
