package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.core.AppLanguage
import com.example.model.AnalyticsLog
import com.example.model.BusinessProfile
import com.example.model.CompetitorEntry
import com.example.model.KeywordTarget
import com.example.model.SeoPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    suspend fun generatePdfReportUri(
        context: Context,
        profile: BusinessProfile,
        logs: List<AnalyticsLog>,
        keywords: List<KeywordTarget>,
        competitors: List<CompetitorEntry>,
        posts: List<SeoPost>
    ): Uri = withContext(Dispatchers.IO) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#047857")
        }
        canvas.drawRect(0f, 0f, 595f, 110f, headerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("RankPulse Local SEO Performance Report", 32f, 48f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D1FAE5")
            textSize = 12f
        }
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        canvas.drawText("${profile.businessName} • Generated $dateStr", 32f, 76f, subPaint)
        canvas.drawText("Owner: ${profile.ownerName} • ${profile.ownerPhone} • ${profile.address}", 32f, 94f, subPaint)

        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#064E3B")
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 11.5f
        }

        var y = 144f
        canvas.drawText("1. Executive Summary (All-Time & Monthly)", 32f, y, sectionPaint)
        y += 22f
        canvas.drawText("All-Time Leads: ${profile.allTimeLeads}   |   All-Time Calls: ${profile.allTimeCalls}   |   All-Time Views: ${profile.allTimeViews}", 32f, y, bodyPaint)
        y += 18f
        canvas.drawText("Monthly Leads: ${profile.monthlyLeads}   |   Monthly Calls: ${profile.monthlyCalls}   |   Monthly Views: ${profile.monthlyViews}", 32f, y, bodyPaint)
        y += 18f
        canvas.drawText("Google Business Profile Status: ${profile.gmbStatus.uppercase()}   |   CTA: ${profile.gmbCtaType}", 32f, y, bodyPaint)

        y += 32f
        canvas.drawText("2. Monthly Analytics Breakdown", 32f, y, sectionPaint)
        y += 20f
        if (logs.isEmpty()) {
            canvas.drawText("No monthly log snapshots recorded yet.", 32f, y, bodyPaint)
            y += 18f
        } else {
            logs.take(6).forEach { log ->
                canvas.drawText("${log.monthLabel}: Leads=${log.leads}, Calls=${log.calls}, Views=${log.views}", 32f, y, bodyPaint)
                y += 18f
            }
        }

        y += 18f
        canvas.drawText("3. Tracked Local Keywords (${keywords.size})", 32f, y, sectionPaint)
        y += 20f
        if (keywords.isEmpty()) {
            canvas.drawText("No tracked keywords yet.", 32f, y, bodyPaint)
            y += 18f
        } else {
            keywords.take(6).forEach { kw ->
                canvas.drawText("• ${kw.keyword} (Rank #${kw.currentRank}, ${kw.intent}, ${kw.difficulty})", 32f, y, bodyPaint)
                y += 18f
            }
        }

        y += 18f
        canvas.drawText("4. Competitor Benchmarks (${competitors.size})", 32f, y, sectionPaint)
        y += 20f
        if (competitors.isEmpty()) {
            canvas.drawText("No competitors added yet.", 32f, y, bodyPaint)
            y += 18f
        } else {
            competitors.take(5).forEach { comp ->
                canvas.drawText(
                    "• ${comp.name} — Rank #${comp.localRank}, ${comp.rating}★ (${comp.reviewCount} rev), ~${comp.monthlyViewsEst} views/mo, SEO ${comp.seoScore}/100",
                    32f,
                    y,
                    bodyPaint
                )
                y += 18f
            }
        }

        y += 18f
        canvas.drawText("5. Recent Branded Social & GMB Posts (${posts.size})", 32f, y, sectionPaint)
        y += 20f
        posts.take(5).forEach { post ->
            canvas.drawText("• [${post.status}] ${post.title.take(55)} (${post.channels.joinToString(",")})", 32f, y, bodyPaint)
            y += 18f
        }

        pdf.finishPage(page)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "RankPulse_SEO_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            pdf.writeTo(out)
        }
        pdf.close()

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun generateCsvReportUri(
        context: Context,
        profile: BusinessProfile,
        logs: List<AnalyticsLog>,
        keywords: List<KeywordTarget>
    ): Uri = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.appendLine("Section,Metric,Value")
        sb.appendLine("Business,Name,\"${profile.businessName.replace("\"", "\"\"")}\"")
        sb.appendLine("Business,GMB Status,${profile.gmbStatus}")
        sb.appendLine("AllTime,Leads,${profile.allTimeLeads}")
        sb.appendLine("AllTime,Calls,${profile.allTimeCalls}")
        sb.appendLine("AllTime,Views,${profile.allTimeViews}")
        sb.appendLine("Monthly,Leads,${profile.monthlyLeads}")
        sb.appendLine("Monthly,Calls,${profile.monthlyCalls}")
        sb.appendLine("Monthly,Views,${profile.monthlyViews}")
        sb.appendLine()
        sb.appendLine("Month,Leads,Calls,Views")
        logs.forEach { log ->
            sb.appendLine("\"${log.monthLabel}\",${log.leads},${log.calls},${log.views}")
        }
        sb.appendLine()
        sb.appendLine("Keyword,Intent,Difficulty,Rank")
        keywords.forEach { kw ->
            sb.appendLine("\"${kw.keyword.replace("\"", "\"\"")}\",${kw.intent},${kw.difficulty},${kw.currentRank}")
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "RankPulse_Analytics_${System.currentTimeMillis()}.csv")
        file.writeText(sb.toString())

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun generateGdprExportUri(
        context: Context,
        profile: BusinessProfile,
        posts: List<SeoPost>,
        keywords: List<KeywordTarget>
    ): Uri = withContext(Dispatchers.IO) {
        val root = JSONObject().apply {
            put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
            put("businessName", profile.businessName)
            put("ownerName", profile.ownerName)
            put("ownerPhone", profile.ownerPhone)
            put("address", profile.address)
            put("gdprConsent", profile.gdprConsent)
            put("e2eeEnabled", profile.e2eeEnabled)
            put("postsCount", posts.size)
            put("keywords", JSONArray(keywords.map { it.keyword }))
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "RankPulse_GDPR_Data_${System.currentTimeMillis()}.json")
        file.writeText(root.toString(2))

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun buildMonthlyWhatsappSummary(
        profile: BusinessProfile,
        language: AppLanguage
    ): String {
        val monthStr = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
        return if (language == AppLanguage.HINDI) {
            """
                📊 *${profile.businessName} — $monthStr मासिक SEO रिपोर्ट*
                
                ✅ *इस महीने का प्रदर्शन:*
                • लीड्स: ${profile.monthlyLeads}
                • कॉल्स: ${profile.monthlyCalls}
                • व्यूज़: ${profile.monthlyViews}
                
                🏆 *कुल (All-Time) प्रदर्शन:*
                • कुल लीड्स: ${profile.allTimeLeads}
                • कुल कॉल्स: ${profile.allTimeCalls}
                • कुल व्यूज़: ${profile.allTimeViews}
                
                📍 Google Business Profile: ${profile.gmbStatus.uppercase()}
                📞 संपर्क: ${profile.ownerPhone}
                🏠 पता: ${profile.address}
            """.trimIndent()
        } else {
            """
                📊 *${profile.businessName} — $monthStr SEO Performance Report*
                
                ✅ *This Month:*
                • Leads: ${profile.monthlyLeads}
                • Calls: ${profile.monthlyCalls}
                • Profile Views: ${profile.monthlyViews}
                
                🏆 *All-Time Totals:*
                • Total Leads: ${profile.allTimeLeads}
                • Total Calls: ${profile.allTimeCalls}
                • Total Views: ${profile.allTimeViews}
                
                📍 Google Business Profile: ${profile.gmbStatus.uppercase()}
                📞 Phone: ${profile.ownerPhone}
                🏠 Address: ${profile.address}
            """.trimIndent()
        }
    }

    fun shareFileAndText(
        context: Context,
        uri: Uri,
        mimeType: String,
        text: String,
        targetPackage: String? = null,
        chooserTitle: String = "Share via"
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (!targetPackage.isNullOrBlank()) {
                setPackage(targetPackage)
            }
        }
        try {
            if (!targetPackage.isNullOrBlank()) {
                context.startActivity(intent)
            } else {
                context.startActivity(Intent.createChooser(intent, chooserTitle))
            }
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallback, chooserTitle))
        }
    }
}
