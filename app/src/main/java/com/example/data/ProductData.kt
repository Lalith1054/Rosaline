package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import kotlin.math.max

@Entity(tableName = "product_entries")
data class ProductEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val isActive: Boolean = true
) {
    /**
     * Returns the elapsed time in milliseconds from startTimeMillis until endTimeMillis (if stopped)
     * or nowMillis (if still running).
     */
    fun elapsedMillis(nowMillis: Long): Long {
        val effectiveEnd = if (isActive || endTimeMillis == null) nowMillis else endTimeMillis
        return max(0L, effectiveEnd - startTimeMillis)
    }

    /**
     * Calculates the number of days the product has been used.
     * If countStartAsDayOne is true, Day 1 begins immediately upon starting.
     */
    fun elapsedDaysCount(nowMillis: Long, countStartAsDayOne: Boolean = false): Long {
        val millis = elapsedMillis(nowMillis)
        val fullDays = TimeUnit.MILLISECONDS.toDays(millis)
        return if (countStartAsDayOne) fullDays + 1L else fullDays
    }

    /**
     * Live human-readable elapsed duration string.
     */
    fun formattedLiveDuration(nowMillis: Long): String {
        val totalSeconds = elapsedMillis(nowMillis) / 1000L
        val days = totalSeconds / 86400L
        val hours = (totalSeconds % 86400L) / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L

        return when {
            days > 0L -> "${days}d ${hours}h ${minutes}m"
            hours > 0L -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0L -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}

enum class AppThemeOption(val displayName: String) {
    CLASSIC_WHITE("Classic White"),
    CHOCOLATE_MILKSHAKE("Chocolate Milkshake"),
    BREAD_OMELETTE("Bread Omelette");

    companion object {
        fun fromName(name: String?): AppThemeOption {
            return entries.firstOrNull { it.name == name } ?: CLASSIC_WHITE
        }
    }
}

data class UserSettings(
    val countStartDayAsDayOne: Boolean = false,
    val showLiveSubDayTimer: Boolean = true,
    val selectedTheme: AppThemeOption = AppThemeOption.CLASSIC_WHITE
)

class SettingsPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rosaline_settings", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(
        UserSettings(
            countStartDayAsDayOne = prefs.getBoolean(KEY_COUNT_DAY_ONE, false),
            showLiveSubDayTimer = prefs.getBoolean(KEY_SHOW_LIVE_TIMER, true),
            selectedTheme = AppThemeOption.fromName(prefs.getString(KEY_SELECTED_THEME, AppThemeOption.CLASSIC_WHITE.name))
        )
    )
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    fun setCountStartDayAsDayOne(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COUNT_DAY_ONE, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(countStartDayAsDayOne = enabled)
    }

    fun setShowLiveSubDayTimer(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_LIVE_TIMER, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(showLiveSubDayTimer = enabled)
    }

    fun setSelectedTheme(theme: AppThemeOption) {
        prefs.edit().putString(KEY_SELECTED_THEME, theme.name).apply()
        _settingsFlow.value = _settingsFlow.value.copy(selectedTheme = theme)
    }

    companion object {
        private const val KEY_COUNT_DAY_ONE = "count_start_day_as_day_one"
        private const val KEY_SHOW_LIVE_TIMER = "show_live_sub_day_timer"
        private const val KEY_SELECTED_THEME = "selected_theme_option"
    }
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM product_entries ORDER BY isActive DESC, startTimeMillis DESC, id DESC")
    fun observeAllProducts(): Flow<List<ProductEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntry): Long

    @Update
    suspend fun updateProduct(product: ProductEntry)

    @Query("DELETE FROM product_entries WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("DELETE FROM product_entries WHERE isActive = 0")
    suspend fun deleteFinishedProducts()

    @Query("DELETE FROM product_entries")
    suspend fun deleteAllProducts()

    @Query("SELECT * FROM product_entries WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntry?
}

@Database(entities = [ProductEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lastdrop_products.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ProductRepository(private val productDao: ProductDao) {
    val allProducts: Flow<List<ProductEntry>> = productDao.observeAllProducts()

    suspend fun startNewProduct(name: String, startTimeMillis: Long = System.currentTimeMillis()): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        val entry = ProductEntry(
            name = trimmed,
            startTimeMillis = startTimeMillis,
            endTimeMillis = null,
            isActive = true
        )
        return productDao.insertProduct(entry)
    }

    suspend fun restoreProduct(product: ProductEntry): Long {
        return productDao.insertProduct(product)
    }

    suspend fun stopProduct(product: ProductEntry, stopTimeMillis: Long = System.currentTimeMillis()) {
        if (!product.isActive) return
        val updated = product.copy(
            endTimeMillis = max(product.startTimeMillis, stopTimeMillis),
            isActive = false
        )
        productDao.updateProduct(updated)
    }

    suspend fun deleteProduct(id: Long) {
        productDao.deleteProductById(id)
    }

    suspend fun clearFinishedProducts() {
        productDao.deleteFinishedProducts()
    }

    suspend fun clearAllProducts() {
        productDao.deleteAllProducts()
    }
}
