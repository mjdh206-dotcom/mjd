package com.mjd.videoforge.di

import android.content.Context
import androidx.room.Room
import com.mjd.videoforge.data.db.*
import com.mjd.videoforge.data.security.ApiKeyEncryption
import com.mjd.videoforge.providers.claude.ClaudeProvider
import com.mjd.videoforge.providers.deepseek.DeepSeekProvider
import com.mjd.videoforge.providers.gemini.GeminiProvider
import com.mjd.videoforge.providers.gpt.GptProvider
import com.mjd.videoforge.providers.grok.GrokProvider
import com.mjd.videoforge.providers.groq.GroqProvider
import com.mjd.videoforge.providers.mistral.MistralProvider
import com.mjd.videoforge.providers.ProviderManager
import com.mjd.videoforge.providers.audio.ElevenLabsProvider
import com.mjd.videoforge.providers.audio.EdgeTtsProvider
import com.mjd.videoforge.providers.image.ReplicateProvider
import com.mjd.videoforge.providers.image.StabilityProvider
import com.mjd.videoforge.providers.music.SunoProvider
import com.mjd.videoforge.providers.video.RunwayProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProvidersModule {

    // ==================== DATABASE ====================

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VideoForgeDatabase {
        return Room.databaseBuilder(
            context,
            VideoForgeDatabase::class.java,
            "videoforge.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideProjectDao(db: VideoForgeDatabase): ProjectDao = db.projectDao()

    @Provides
    fun provideSceneDao(db: VideoForgeDatabase): SceneDao = db.sceneDao()

    @Provides
    fun provideProviderDao(db: VideoForgeDatabase): ProviderDao = db.providerDao()

    @Provides
    fun provideQueueDao(db: VideoForgeDatabase): QueueDao = db.queueDao()

    @Provides
    fun provideCostDao(db: VideoForgeDatabase): CostDao = db.costDao()

    @Provides
    fun provideBrandDao(db: VideoForgeDatabase): BrandDao = db.brandDao()

    @Provides
    fun provideTemplateDao(db: VideoForgeDatabase): TemplateDao = db.templateDao()

    @Provides
    fun provideAssetDao(db: VideoForgeDatabase): AssetDao = db.assetDao()

    // ==================== NETWORK ====================

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    // ==================== SECURITY ====================

    @Provides
    @Singleton
    fun provideApiKeyEncryption(@ApplicationContext context: Context): ApiKeyEncryption {
        return ApiKeyEncryption(context)
    }

    // ==================== PROVIDER REGISTRATION ====================

    @Provides
    @Singleton
    fun provideProviderManager(
        providerDao: ProviderDao,
        costTracker: CostTracker
    ): ProviderManager {
        return ProviderManager(providerDao, costTracker)
    }

    /**
     * تهيئة كل المحركات عند بدء التطبيق
     */
    @Provides
    @Singleton
    fun provideInitializedProviders(
        providerManager: ProviderManager,
        apiKeyEncryption: ApiKeyEncryption,
        httpClient: OkHttpClient
    ): ProviderManager {
        // Text providers (by priority - free first)
        providerManager.registerProvider(
            GeminiProvider(apiKeyEncryption, httpClient)
        )
        providerManager.registerProvider(
            DeepSeekProvider(apiKeyEncryption, httpClient)
        )
        providerManager.registerProvider(
            GroqProvider(apiKeyEncryption, httpClient)
        )
        providerManager.registerProvider(
            MistralProvider(apiKeyEncryption, httpClient)
        )
        providerManager.registerProvider(
            ClaudeProvider(apiKeyEncryption, httpClient)
        )
        providerManager.registerProvider(
            GptProvider(apiKeyEncryption, httpClient)
        )
        providerManager.registerProvider(
            GrokProvider(apiKeyEncryption, httpClient)
        )

        // Image providers
        providerManager.registerProvider(
            StabilityProvider(apiKeyEncryption, httpClient)
        )
        providerManager.registerProvider(
            ReplicateProvider(apiKeyEncryption, httpClient)
        )
        // GPT also handles images (DALL-E)

        // Audio/TTS providers
        providerManager.registerProvider(EdgeTtsProvider()) // Free, no key needed
        providerManager.registerProvider(
            ElevenLabsProvider(apiKeyEncryption, httpClient)
        )
        // GPT also handles TTS

        // Video providers
        providerManager.registerProvider(
            RunwayProvider(apiKeyEncryption, httpClient)
        )

        // Music providers
        providerManager.registerProvider(
            SunoProvider(apiKeyEncryption, httpClient)
        )

        return providerManager
    }
}
