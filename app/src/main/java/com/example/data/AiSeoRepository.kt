package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.core.AppLanguage
import com.example.model.BusinessProfile
import com.example.model.CompetitorEntry
import com.example.model.CustomerReview
import com.example.model.KeywordTarget
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

private const val TAG = "AiSeoRepository"

data class AiBrandIdentityResult(
    val tagline: String,
    val seoDescription: String,
    val ctaLabel: String,
    val logoConcept: String
)

data class AiKeywordSuggestion(
    val keyword: String,
    val intent: String,
    val difficulty: String,
    val aiTip: String
)

class AiSeoRepository {

    private val textModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val brandSchema = Schema.obj(
        properties = mapOf(
            "tagline" to Schema.string("Short punchy local SEO tagline"),
            "seoDescription" to Schema.string("Conversion-focused local SEO business description (max 400 chars)"),
            "ctaLabel" to Schema.string("Recommended Google Business Profile CTA button text"),
            "logoConcept" to Schema.string("Concise visual logo design brief")
        )
    )

    private val brandModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = brandSchema
            }
        )
    }

    private val keywordsSchema = Schema.obj(
        properties = mapOf(
            "keywords" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "keyword" to Schema.string("Local SEO keyword phrase"),
                        "intent" to Schema.string("Transactional, Local Map, or Informational"),
                        "difficulty" to Schema.string("Low, Medium, or High"),
                        "aiTip" to Schema.string("Actionable placement tip")
                    )
                ),
                description = "5 high-converting local SEO keywords"
            )
        )
    )

    private val keywordsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = keywordsSchema
            }
        )
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    private fun languageInstruction(language: AppLanguage): String =
        if (language == AppLanguage.HINDI) {
            "Respond in natural, professional Hindi (Devanagari script) suitable for Indian local business owners."
        } else {
            "Respond in clear, high-converting English."
        }

    suspend fun optimizeWebsiteCopy(
        profile: BusinessProfile,
        pageTopic: String,
        existingCopy: String,
        language: AppLanguage
    ): String {
        val prompt = """
            You are a senior Local SEO copywriter.
            Business: ${profile.businessName} (${profile.category})
            Location: ${profile.address.ifBlank { "Local area" }}
            Website: ${profile.websiteUrl.ifBlank { "N/A" }}
            Topic/Page: $pageTopic
            Existing Copy: ${existingCopy.ifBlank { "None provided — draft from scratch" }}
            
            Provide:
            1. SEO Title Tag (max 60 chars)
            2. Meta Description (max 155 chars)
            3. H1 Headline
            4. Optimized Website Copy (2 concise paragraphs with local keywords and strong call-to-action calling ${profile.ownerPhone.ifBlank { "the business" }}).
            ${languageInstruction(language)}
        """.trimIndent()

        return withTimeout(60_000L) {
            textModel.generateContent(prompt).text
                ?: throw IllegalStateException("Gemini returned an empty response.")
        }
    }

    suspend fun generateKeywordRecommendations(
        profile: BusinessProfile,
        seedTopic: String,
        language: AppLanguage
    ): List<AiKeywordSuggestion> {
        val prompt = """
            Generate 5 high-intent local SEO keyword recommendations for:
            Business: ${profile.businessName}
            Category: ${profile.category}
            Address/City: ${profile.address.ifBlank { "Local city" }}
            Focus: ${seedTopic.ifBlank { profile.category }}
            ${languageInstruction(language)}
        """.trimIndent()

        val jsonText = withTimeout(60_000L) {
            keywordsModel.generateContent(prompt).text
                ?: throw IllegalStateException("Gemini returned empty keyword JSON.")
        }

        val root = JSONObject(jsonText)
        val arr = root.getJSONArray("keywords")
        return (0 until arr.length()).map { idx ->
            val obj = arr.getJSONObject(idx)
            AiKeywordSuggestion(
                keyword = obj.optString("keyword", "local service").take(120),
                intent = obj.optString("intent", "Transactional").take(40),
                difficulty = obj.optString("difficulty", "Medium").take(40),
                aiTip = obj.optString("aiTip", "Add to H1 and GMB description.").take(400)
            )
        }
    }

    suspend fun generateBrandDescriptionAndCta(
        profile: BusinessProfile,
        language: AppLanguage
    ): AiBrandIdentityResult {
        val prompt = """
            Create a Google Business Profile brand identity kit for:
            Business Name: ${profile.businessName}
            Category: ${profile.category}
            Address: ${profile.address.ifBlank { "Local neighborhood" }}
            Phone: ${profile.ownerPhone.ifBlank { "Available on profile" }}
            ${languageInstruction(language)}
        """.trimIndent()

        val jsonText = withTimeout(60_000L) {
            brandModel.generateContent(prompt).text
                ?: throw IllegalStateException("Gemini returned empty brand JSON.")
        }

        val obj = JSONObject(jsonText)
        return AiBrandIdentityResult(
            tagline = obj.optString("tagline", "${profile.businessName} — Trusted Local Experts"),
            seoDescription = obj.optString("seoDescription", profile.description),
            ctaLabel = obj.optString("ctaLabel", "Call Now"),
            logoConcept = obj.optString("logoConcept", "Modern emerald geometric emblem")
        )
    }

    suspend fun generateDailyPostCopy(
        profile: BusinessProfile,
        topic: String,
        workPerformedBy: String,
        language: AppLanguage
    ): Pair<String, String> {
        val prompt = """
            Write a short, punchy local social media & Google Business Profile update post.
            Business: ${profile.businessName} (${profile.category})
            Work/Topic: $topic
            Work Performed By: $workPerformedBy
            Address: ${profile.address}
            Phone: ${profile.ownerPhone}
            Return ONLY two lines:
            LINE 1: Catchy headline (max 70 chars)
            LINE 2: Engaging description with local SEO keywords and CTA (max 240 chars)
            ${languageInstruction(language)}
        """.trimIndent()

        val raw = withTimeout(60_000L) {
            textModel.generateContent(prompt).text
                ?: throw IllegalStateException("Gemini returned empty post copy.")
        }
        val lines = raw.lines().map { it.trim().removePrefix("LINE 1:").removePrefix("LINE 2:").trim() }
            .filter { it.isNotBlank() }
        val headline = lines.firstOrNull()?.take(120) ?: topic.take(120)
        val desc = lines.drop(1).joinToString(" ").ifBlank { raw }.take(1200)
        return headline to desc
    }

    suspend fun generateReviewReply(
        profile: BusinessProfile,
        review: CustomerReview,
        language: AppLanguage
    ): String {
        val prompt = """
            Write a warm, SEO-friendly business owner reply to a Google Business Profile customer review.
            Business Name: ${profile.businessName}
            Owner Name: ${profile.ownerName}
            Reviewer: ${review.reviewerName}
            Rating: ${review.rating}/5 stars
            Review: "${review.comment}"
            Keep it under 320 characters, mention the business category naturally, and include a polite invitation back.
            ${languageInstruction(language)}
        """.trimIndent()

        return withTimeout(60_000L) {
            textModel.generateContent(prompt).text?.trim()
                ?: throw IllegalStateException("Gemini returned an empty reply.")
        }
    }

    suspend fun analyzeCompetitorGap(
        profile: BusinessProfile,
        competitor: CompetitorEntry,
        userLocalRank: Long,
        userAvgRating: Double,
        userReviewCount: Long,
        userSeoScore: Long,
        language: AppLanguage
    ): String {
        val ratingStr = if (userAvgRating > 0.0) String.format(java.util.Locale.US, "%.1f★", userAvgRating) else "No reviews yet"
        val prompt = """
            Perform a sharp local SEO competitive gap analysis comparing Our Business vs. Competitor.
            Our Business: ${profile.businessName} (${profile.category}), Location: ${profile.address.ifBlank { "Local area" }}
            - Our Metrics: Local Pack Rank #$userLocalRank, Avg Rating $ratingStr ($userReviewCount reviews), Monthly Views ${profile.monthlyViews}, SEO Score $userSeoScore/100.
            Competitor: ${competitor.name}, Website: ${competitor.website.ifBlank { "N/A" }}
            - Competitor Metrics: Local Pack Rank #${competitor.localRank}, Rating ${competitor.rating}★ (${competitor.reviewCount} reviews), Est. Monthly Views ${competitor.monthlyViewsEst}, SEO Score ${competitor.seoScore}/100.
            Give 3 concise, high-impact actionable steps to close the gap and outrank ${competitor.name} on Google Maps and local organic search.
            ${languageInstruction(language)}
        """.trimIndent()

        return withTimeout(60_000L) {
            textModel.generateContent(prompt).text?.trim()
                ?: throw IllegalStateException("Gemini returned an empty analysis.")
        }
    }

    fun startSupportChat(profile: BusinessProfile, language: AppLanguage): Chat {
        val systemPrimer = """
            You are RankPulse AI Support, an expert assistant for local businesses using RankPulse SEO.
            Business context: ${profile.businessName}, Category: ${profile.category}, GMB status: ${profile.gmbStatus}.
            Help the user with Google Business Profile setup/linking, local SEO ranking, daily branded posts, WhatsApp reports, and troubleshooting.
            ${languageInstruction(language)}
        """.trimIndent()
        return textModel.startChat(
            history = listOf(
                content("user") { text(systemPrimer) },
                content("model") {
                    text(
                        if (language == AppLanguage.HINDI) {
                            "नमस्ते! मैं RankPulse AI सपोर्ट हूँ। मैं आपकी Google Business Profile, लोकल SEO और पोस्टर शेड्यूलिंग में कैसे मदद कर सकता हूँ?"
                        } else {
                            "Hi! I'm RankPulse AI Support. How can I help boost your Google Business Profile, local rankings, or daily posts today?"
                        }
                    )
                }
            )
        )
    }

    fun sendChatMessageStream(chat: Chat, message: String): Flow<String> = flow {
        val accumulator = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(message).collect { chunk ->
                val textPart = chunk.text.orEmpty()
                if (textPart.isNotEmpty()) {
                    accumulator.append(textPart)
                    emit(accumulator.toString())
                }
            }
        }
    }

    suspend fun generateAiImageAndSave(
        context: Context,
        promptText: String,
        filePrefix: String
    ): String {
        val bitmap = withTimeout(60_000L) {
            val response = imageModel.generateContent(promptText)
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
            imagePart?.image ?: throw IllegalStateException("No image was returned by Gemini.")
        }
        return saveBitmapInternal(context, bitmap, filePrefix)
    }

    companion object {
        suspend fun saveBitmapInternal(
            context: Context,
            bitmap: Bitmap,
            filePrefix: String
        ): String = withContext(Dispatchers.IO) {
            val dir = File(context.applicationContext.filesDir, "brand_media").apply { mkdirs() }
            val outFile = File(dir, "${filePrefix}_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            outFile.absolutePath
        }

        suspend fun copyPickerUriToInternal(
            context: Context,
            uri: Uri,
            filePrefix: String
        ): String = withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val dir = File(appContext.filesDir, "brand_media").apply { mkdirs() }
            val outFile = File(dir, "${filePrefix}_${System.currentTimeMillis()}.jpg")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Unable to read selected image.")
            outFile.absolutePath
        }
    }
}
