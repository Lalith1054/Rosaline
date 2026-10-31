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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import kotlin.math.max

val DefaultProductTags = listOf(
    "Bodywash",
    "Shampoo",
    "Conditioner",
    "Toothpaste",
    "Face Cleanser",
    "Body Lotion",
    "Hand Soap",
    "Dish Soap"
)

@Entity(tableName = "product_entries")
data class ProductEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val isActive: Boolean = true,
    val tag: String? = null
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
}

enum class AppThemeOption(val displayName: String) {
    CLASSIC_WHITE("Classic White"),
    CHOCOLATE_MILKSHAKE("A Certain Chocolate Milkshake"),
    BREAD_OMELETTE("The Usual Bread Omelette");

    companion object {
        fun fromName(name: String?): AppThemeOption {
            return entries.firstOrNull { it.name == name } ?: CLASSIC_WHITE
        }
    }
}

data class UserSettings(
    val countStartDayAsDayOne: Boolean = false,
    val selectedTheme: AppThemeOption = AppThemeOption.CLASSIC_WHITE,
    val darkModeEnabled: Boolean = false,
    val githubRepo: String = com.example.BuildConfig.GITHUB_REPOSITORY.ifBlank { "lalith0918/Rosaline" },
    val customTags: List<String> = emptyList()
) {
    val allAvailableTags: List<String>
        get() = (DefaultProductTags + customTags).distinctBy { it.lowercase() }
}

class SettingsPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("rosaline_settings", Context.MODE_PRIVATE)

    private val defaultRepo = com.example.BuildConfig.GITHUB_REPOSITORY.ifBlank { "lalith0918/Rosaline" }

    private fun loadCustomTags(): List<String> {
        val raw = prefs.getString(KEY_CUSTOM_TAGS, "") ?: ""
        return raw.split(CUSTOM_TAG_DELIMITER)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
    }

    private val _settingsFlow = MutableStateFlow(
        UserSettings(
            countStartDayAsDayOne = prefs.getBoolean(KEY_COUNT_DAY_ONE, false),
            selectedTheme = AppThemeOption.fromName(prefs.getString(KEY_SELECTED_THEME, AppThemeOption.CLASSIC_WHITE.name)),
            darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false),
            githubRepo = prefs.getString(KEY_GITHUB_REPO, defaultRepo)?.ifBlank { defaultRepo } ?: defaultRepo,
            customTags = loadCustomTags()
        )
    )
    val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    fun setCountStartDayAsDayOne(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COUNT_DAY_ONE, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(countStartDayAsDayOne = enabled)
    }

    fun setSelectedTheme(theme: AppThemeOption) {
        prefs.edit()
            .putString(KEY_SELECTED_THEME, theme.name)
            .putBoolean(KEY_DARK_MODE, false)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            selectedTheme = theme,
            darkModeEnabled = false
        )
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(darkModeEnabled = enabled)
    }

    fun setGithubRepo(repo: String) {
        val cleaned = repo.trim().ifBlank { defaultRepo }
        prefs.edit().putString(KEY_GITHUB_REPO, cleaned).apply()
        _settingsFlow.value = _settingsFlow.value.copy(githubRepo = cleaned)
    }

    fun addCustomTag(newTag: String) {
        val cleaned = newTag.trim()
        if (cleaned.isEmpty()) return
        val existingAll = (DefaultProductTags + _settingsFlow.value.customTags).map { it.lowercase() }
        if (cleaned.lowercase() in existingAll) return

        val updatedCustom = _settingsFlow.value.customTags + cleaned
        prefs.edit()
            .putString(KEY_CUSTOM_TAGS, updatedCustom.joinToString(CUSTOM_TAG_DELIMITER))
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(customTags = updatedCustom)
    }

    companion object {
        private const val KEY_COUNT_DAY_ONE = "count_start_day_as_day_one"
        private const val KEY_SELECTED_THEME = "selected_theme_option"
        private const val KEY_DARK_MODE = "dark_mode_enabled"
        private const val KEY_GITHUB_REPO = "github_release_repo"
        private const val KEY_CUSTOM_TAGS = "custom_product_tags"
        private const val CUSTOM_TAG_DELIMITER = "|||"
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

@Database(entities = [ProductEntry::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE product_entries ADD COLUMN tag TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lastdrop_products.db"
                )
                    .addMigrations(MIGRATION_1_2)
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

    suspend fun startNewProduct(
        name: String,
        startTimeMillis: Long = System.currentTimeMillis(),
        tag: String? = null
    ): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        val cleanedTag = tag?.trim()?.takeIf { it.isNotEmpty() }
        val entry = ProductEntry(
            name = trimmed,
            startTimeMillis = startTimeMillis,
            endTimeMillis = null,
            isActive = true,
            tag = cleanedTag
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
