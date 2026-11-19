package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class BusinessProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val businessName: String = "My Local Business",
    val category: String = "Local Service",
    val ownerName: String = "Business Owner",
    val ownerPhone: String = "",
    val address: String = "",
    val websiteUrl: String = "",
    val description: String = "",
    val ownerPhotoPath: String = "",
    val serviceLogoPath: String = "",
    val gmbStatus: String = "not_linked", // not_linked | creating | linked
    val gmbPlaceUrl: String = "",
    val gmbCtaType: String = "CALL", // CALL | BOOK | LEARN_MORE | ORDER | SIGN_UP
    val whatsappNumber: String = "",
    val instagramHandle: String = "",
    val facebookPage: String = "",
    val autoReplyReviews: Boolean = true,
    val dailyAutoPost: Boolean = true,
    val monthlyWhatsappReport: Boolean = true,
    val gdprConsent: Boolean = true,
    val e2eeEnabled: Boolean = true,
    val workspaceRole: String = "OWNER",
    val subscriptionTier: String = "TRIAL", // TRIAL | STARTER | GROWTH | ENTERPRISE
    val billingCycle: String = "MONTHLY", // MONTHLY | HALF_YEARLY | ANNUAL
    val trialActive: Boolean = true,
    val paymentMethodLabel: String = "14-Day Free Trial",
    val allTimeLeads: Long = 0L,
    val allTimeCalls: Long = 0L,
    val allTimeViews: Long = 0L,
    val monthlyLeads: Long = 0L,
    val monthlyCalls: Long = 0L,
    val monthlyViews: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "businessName" to businessName.take(100).ifBlank { "My Local Business" },
        "category" to category.take(80).ifBlank { "Local Service" },
        "ownerName" to ownerName.take(80).ifBlank { "Business Owner" },
        "ownerPhone" to ownerPhone.take(30),
        "address" to address.take(200),
        "websiteUrl" to websiteUrl.take(200),
        "description" to description.take(1000),
        "ownerPhotoPath" to ownerPhotoPath.take(500),
        "serviceLogoPath" to serviceLogoPath.take(500),
        "gmbStatus" to (if (gmbStatus in listOf("not_linked", "creating", "linked")) gmbStatus else "not_linked"),
        "gmbPlaceUrl" to gmbPlaceUrl.take(300),
        "gmbCtaType" to (if (gmbCtaType in listOf("CALL", "BOOK", "LEARN_MORE", "ORDER", "SIGN_UP")) gmbCtaType else "CALL"),
        "whatsappNumber" to whatsappNumber.take(30),
        "instagramHandle" to instagramHandle.take(80),
        "facebookPage" to facebookPage.take(80),
        "autoReplyReviews" to autoReplyReviews,
        "dailyAutoPost" to dailyAutoPost,
        "monthlyWhatsappReport" to monthlyWhatsappReport,
        "gdprConsent" to gdprConsent,
        "e2eeEnabled" to e2eeEnabled,
        "workspaceRole" to "OWNER",
        "subscriptionTier" to (if (subscriptionTier in listOf("TRIAL", "STARTER", "GROWTH", "ENTERPRISE")) subscriptionTier else "TRIAL"),
        "billingCycle" to (if (billingCycle in listOf("MONTHLY", "HALF_YEARLY", "ANNUAL")) billingCycle else "MONTHLY"),
        "trialActive" to trialActive,
        "paymentMethodLabel" to paymentMethodLabel.take(80),
        "allTimeLeads" to allTimeLeads.coerceAtLeast(0L),
        "allTimeCalls" to allTimeCalls.coerceAtLeast(0L),
        "allTimeViews" to allTimeViews.coerceAtLeast(0L),
        "monthlyLeads" to monthlyLeads.coerceAtLeast(0L),
        "monthlyCalls" to monthlyCalls.coerceAtLeast(0L),
        "monthlyViews" to monthlyViews.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class SeoPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val workPerformedBy: String = "Business Team",
    val channels: List<String> = listOf("WHATSAPP", "INSTAGRAM", "FACEBOOK", "GMB", "WEBSITE"),
    val status: String = "SCHEDULED", // DRAFT | SCHEDULED | PUBLISHED
    val scheduledDateLabel: String = "Daily 09:00 AM",
    val ctaLabel: String = "Call Now",
    val posterAccentHex: String = "#047857",
    val posterImagePath: String = "",
    val ownerPhotoSnapshotPath: String = "",
    val serviceLogoSnapshotPath: String = "",
    val ownerPhoneSnapshot: String = "",
    val addressSnapshot: String = "",
    val viewsCount: Long = 0L,
    val leadsCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "title" to title.take(120).ifBlank { "Local Service Update" },
        "description" to description.take(1200).ifBlank { "Quality local service available today." },
        "workPerformedBy" to workPerformedBy.take(80).ifBlank { "Business Team" },
        "channels" to channels.take(5).ifEmpty { listOf("GMB") },
        "status" to (if (status in listOf("DRAFT", "SCHEDULED", "PUBLISHED")) status else "SCHEDULED"),
        "scheduledDateLabel" to scheduledDateLabel.take(40),
        "ctaLabel" to ctaLabel.take(40),
        "posterAccentHex" to posterAccentHex.take(16).ifBlank { "#047857" },
        "posterImagePath" to posterImagePath.take(500),
        "ownerPhotoSnapshotPath" to ownerPhotoSnapshotPath.take(500),
        "serviceLogoSnapshotPath" to serviceLogoSnapshotPath.take(500),
        "ownerPhoneSnapshot" to ownerPhoneSnapshot.take(30),
        "addressSnapshot" to addressSnapshot.take(200),
        "viewsCount" to viewsCount.coerceAtLeast(0L),
        "leadsCount" to leadsCount.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CustomerReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reviewerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val replyText: String = "",
    val replied: Boolean = false,
    val autoReplied: Boolean = false,
    val source: String = "Google Business Profile",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "reviewerName" to reviewerName.take(80).ifBlank { "Local Customer" },
        "rating" to rating.coerceIn(1L, 5L),
        "comment" to comment.take(1000).ifBlank { "Great service!" },
        "replyText" to replyText.take(1000),
        "replied" to replied,
        "autoReplied" to autoReplied,
        "source" to source.take(40).ifBlank { "Google Business Profile" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CompetitorEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val website: String = "",
    val localRank: Long = 1L,
    val rating: Double = 4.2,
    val reviewCount: Long = 0L,
    val monthlyViewsEst: Long = 0L,
    val seoScore: Long = 50L,
    val aiGapSummary: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.take(100).ifBlank { "Local Competitor" },
        "website" to website.take(200),
        "localRank" to localRank.coerceIn(1L, 100L),
        "rating" to rating.coerceIn(0.0, 5.0),
        "reviewCount" to reviewCount.coerceAtLeast(0L),
        "monthlyViewsEst" to monthlyViewsEst.coerceAtLeast(0L),
        "seoScore" to seoScore.coerceIn(0L, 100L),
        "aiGapSummary" to aiGapSummary.take(2000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class KeywordTarget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val keyword: String = "",
    val intent: String = "Transactional",
    val difficulty: String = "Medium",
    val currentRank: Long = 10L,
    val aiTip: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "keyword" to keyword.take(120).ifBlank { "local service near me" },
        "intent" to intent.take(40).ifBlank { "Transactional" },
        "difficulty" to difficulty.take(40).ifBlank { "Medium" },
        "currentRank" to currentRank.coerceIn(0L, 100L),
        "aiTip" to aiTip.take(400),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class TeamMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memberName: String = "",
    val memberEmail: String = "",
    val memberRole: String = "EDITOR", // MANAGER | EDITOR | VIEWER
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "memberName" to memberName.take(80).ifBlank { "Team Member" },
        "memberEmail" to memberEmail.take(120).ifBlank { "team@example.com" },
        "memberRole" to (if (memberRole in listOf("MANAGER", "EDITOR", "VIEWER")) memberRole else "EDITOR"),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class SystemNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val severity: String = "INFO", // INFO | WARNING | CRITICAL
    val readFlag: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "title" to title.take(120).ifBlank { "SEO Update" },
        "message" to message.take(500).ifBlank { "System update available." },
        "severity" to (if (severity in listOf("INFO", "WARNING", "CRITICAL")) severity else "INFO"),
        "readFlag" to readFlag,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AnalyticsLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val monthLabel: String = "",
    val leads: Long = 0L,
    val calls: Long = 0L,
    val views: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "monthLabel" to monthLabel.take(30).ifBlank { "Nov 2026" },
        "leads" to leads.coerceAtLeast(0L),
        "calls" to calls.coerceAtLeast(0L),
        "views" to views.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
