package com.mjd.videoforge.data.db

import androidx.room.*
import com.mjd.videoforge.data.model.*
import kotlinx.coroutines.flow.Flow

// ==================== DAOs ====================

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): Project?

    @Query("SELECT * FROM projects WHERE status = :status ORDER BY updatedAt DESC")
    fun getProjectsByStatus(status: ProjectStatus): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE status = 'DRAFT' ORDER BY updatedAt DESC")
    fun getDrafts(): Flow<List<Project>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("SELECT COUNT(*) FROM projects")
    fun getProjectCount(): Flow<Int>

    @Query("SELECT SUM(actualCost) FROM projects WHERE completedAt IS NOT NULL")
    fun getTotalCost(): Flow<Double?>
}

@Dao
interface SceneDao {
    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sceneNumber ASC")
    fun getScenesForProject(projectId: String): Flow<List<Scene>>

    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY sceneNumber ASC")
    suspend fun getScenesForProjectOnce(projectId: String): List<Scene>

    @Query("SELECT * FROM scenes WHERE id = :id")
    suspend fun getSceneById(id: String): Scene?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: Scene)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<Scene>)

    @Update
    suspend fun updateScene(scene: Scene)

    @Delete
    suspend fun deleteScene(scene: Scene)

    @Query("DELETE FROM scenes WHERE projectId = :projectId")
    suspend fun deleteScenesForProject(projectId: String)

    @Query("SELECT COUNT(*) FROM scenes WHERE projectId = :projectId AND isGenerated = 1")
    fun getGeneratedSceneCount(projectId: String): Flow<Int>
}

@Dao
interface ProviderDao {
    @Query("SELECT * FROM provider_configs ORDER BY priority ASC")
    fun getAllProviders(): Flow<List<ProviderConfig>>

    @Query("SELECT * FROM provider_configs ORDER BY priority ASC")
    suspend fun getAllProvidersOnce(): List<ProviderConfig>

    @Query("SELECT * FROM provider_configs WHERE id = :id")
    suspend fun getProviderById(id: String): ProviderConfig?

    @Query("SELECT * FROM provider_configs WHERE type = :type")
    suspend fun getProvidersByType(type: ProviderType): List<ProviderConfig>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(config: ProviderConfig)

    @Update
    suspend fun updateProvider(config: ProviderConfig)

    @Delete
    suspend fun deleteProvider(config: ProviderConfig)

    @Query("UPDATE provider_configs SET dailyUsage = 0, status = 'ACTIVE', cooldownUntil = NULL WHERE lastUsageDate != date('now')")
    suspend fun resetDailyUsage()

    @Query("UPDATE provider_configs SET dailyUsage = dailyUsage + 1, lastUsageDate = date('now') WHERE id = :id")
    suspend fun incrementUsage(id: String)

    @Query("UPDATE provider_configs SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: ProviderStatus)
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    fun getAllQueueItems(): Flow<List<QueueItem>>

    @Query("SELECT * FROM queue_items WHERE status = 'WAITING' ORDER BY position ASC LIMIT 1")
    suspend fun getNextQueueItem(): QueueItem?

    @Query("SELECT * FROM queue_items WHERE id = :id")
    suspend fun getQueueItemById(id: String): QueueItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: QueueItem)

    @Update
    suspend fun updateQueueItem(item: QueueItem)

    @Delete
    suspend fun deleteQueueItem(item: QueueItem)

    @Query("DELETE FROM queue_items WHERE status IN ('COMPLETED', 'CANCELLED')")
    suspend fun clearCompleted()

    @Query("DELETE FROM queue_items")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM queue_items WHERE status = 'WAITING'")
    fun getWaitingCount(): Flow<Int>
}

@Dao
interface CostDao {
    @Query("SELECT * FROM cost_entries ORDER BY timestamp DESC")
    fun getAllCosts(): Flow<List<CostEntry>>

    @Query("SELECT * FROM cost_entries WHERE projectId = :projectId")
    fun getCostsForProject(projectId: String): Flow<List<CostEntry>>

    @Query("SELECT SUM(cost) FROM cost_entries WHERE date(timestamp) = date('now')")
    fun getTodayCost(): Flow<Double?>

    @Query("SELECT SUM(cost) FROM cost_entries WHERE providerId = :providerId AND date(timestamp) = date('now')")
    fun getProviderTodayCost(providerId: String): Flow<Double?>

    @Insert
    suspend fun insertCostEntry(entry: CostEntry)

    @Query("SELECT providerId, SUM(cost) as total FROM cost_entries WHERE date(timestamp) = date('now') GROUP BY providerId")
    fun getTodayUsageByProvider(): Flow<List<ProviderDailyUsage>>

    @Query("SELECT SUM(cost) FROM cost_entries")
    fun getTotalCost(): Flow<Double?>
}

data class ProviderDailyUsage(
    val providerId: String,
    val total: Double
)

@Dao
interface BrandDao {
    @Query("SELECT * FROM brand_kits LIMIT 1")
    fun getBrandKit(): Flow<BrandKit?>

    @Query("SELECT * FROM brand_kits WHERE id = :id")
    suspend fun getBrandKitById(id: String): BrandKit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrandKit(kit: BrandKit)

    @Update
    suspend fun updateBrandKit(kit: BrandKit)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY category ASC")
    fun getAllTemplates(): Flow<List<VideoTemplate>>

    @Query("SELECT * FROM templates WHERE category = :category")
    fun getTemplatesByCategory(category: TemplateCategory): Flow<List<VideoTemplate>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: String): VideoTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: VideoTemplate)

    @Delete
    suspend fun deleteTemplate(template: VideoTemplate)
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY createdAt DESC")
    fun getAllAssets(): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE type = :type")
    fun getAssetsByType(type: AssetType): Flow<List<Asset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: Asset)

    @Update
    suspend fun updateAsset(asset: Asset)

    @Delete
    suspend fun deleteAsset(asset: Asset)
}

// ==================== DATABASE ====================

@Database(
    entities = [
        Project::class,
        Scene::class,
        ProviderConfig::class,
        QueueItem::class,
        CostEntry::class,
        BrandKit::class,
        VideoTemplate::class,
        Asset::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VideoForgeDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun sceneDao(): SceneDao
    abstract fun providerDao(): ProviderDao
    abstract fun queueDao(): QueueDao
    abstract fun costDao(): CostDao
    abstract fun brandDao(): BrandDao
    abstract fun templateDao(): TemplateDao
    abstract fun assetDao(): AssetDao
}
