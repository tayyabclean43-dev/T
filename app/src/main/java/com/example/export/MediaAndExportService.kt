package com.example.export

import android.content.Context
import android.net.Uri
import com.example.core.AppLanguage
import com.example.core.NotificationHelper
import com.example.data.AiSeoRepository
import com.example.model.AnalyticsLog
import com.example.model.BusinessProfile
import com.example.model.CompetitorEntry
import com.example.model.KeywordTarget
import com.example.model.SeoPost

class MediaAndExportService(context: Context) {
    private val appContext = context.applicationContext

    suspend fun persistPickedMedia(uri: Uri, prefix: String): String =
        AiSeoRepository.copyPickerUriToInternal(appContext, uri, prefix)

    suspend fun generateAndSaveAiImage(
        aiRepo: AiSeoRepository,
        prompt: String,
        prefix: String
    ): String = aiRepo.generateAiImageAndSave(appContext, prompt, prefix)

    suspend fun renderPosterUri(post: SeoPost, profile: BusinessProfile): Uri =
        PosterBitmapRenderer.renderBrandedPosterToUri(appContext, post, profile)

    suspend fun downloadPosterToDevice(post: SeoPost, profile: BusinessProfile): String =
        PosterBitmapRenderer.saveBrandedPosterToGallery(appContext, post, profile)

    suspend fun exportPdfUri(
        profile: BusinessProfile,
        logs: List<AnalyticsLog>,
        keywords: List<KeywordTarget>,
        competitors: List<CompetitorEntry>,
        posts: List<SeoPost>
    ): Uri = ReportExporter.generatePdfReportUri(
        appContext, profile, logs, keywords, competitors, posts
    )

    suspend fun exportCsvUri(
        profile: BusinessProfile,
        logs: List<AnalyticsLog>,
        keywords: List<KeywordTarget>
    ): Uri = ReportExporter.generateCsvReportUri(appContext, profile, logs, keywords)

    suspend fun exportGdprUri(
        profile: BusinessProfile,
        posts: List<SeoPost>,
        keywords: List<KeywordTarget>
    ): Uri = ReportExporter.generateGdprExportUri(appContext, profile, posts, keywords)

    fun notifySystemAlert(title: String, message: String, id: Int) {
        NotificationHelper.showSystemAlert(appContext, title, message, id)
    }
}
