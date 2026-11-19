package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.AnalyticsLog
import com.example.model.BusinessProfile
import com.example.model.CompetitorEntry
import com.example.model.CustomerReview
import com.example.model.KeywordTarget
import com.example.model.SeoPost
import com.example.model.SystemNotification
import com.example.model.TeamMember
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SeoRepository private constructor(
    private val appContext: Context?,
    private val injectedDb: FirebaseFirestore?,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(
        db: FirebaseFirestore,
        auth: FirebaseAuth = Firebase.auth
    ) : this(appContext = null, injectedDb = db, auth = auth)

    constructor(context: Context) : this(
        appContext = context.applicationContext,
        injectedDb = null,
        auth = Firebase.auth
    )

    @Volatile
    private var cachedDb: FirebaseFirestore? = injectedDb
    private val dbInitMutex = Mutex()

    private suspend fun db(): FirebaseFirestore {
        cachedDb?.let { return it }
        return dbInitMutex.withLock {
            cachedDb?.let { return@withLock it }
            val ctx = checkNotNull(appContext) { "Context or FirebaseFirestore required" }
            val initialized = withContext(Dispatchers.IO) {
                createConfiguredFirestore(ctx)
            }
            cachedDb = initialized
            initialized
        }
    }

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user changed; refusing cross-user operation." }
        }
        return uid
    }

    private suspend fun userDoc(uid: String) = db().collection("users").document(uid)

    fun observeBusinessProfile(uid: String): Flow<BusinessProfile?> = flow {
        requireUserId(uid)
        val docRef = userDoc(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            BusinessProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error -> handleStreamError(error, OperationType.GET, docRef.path, uid) }
        )
    }

    fun observePosts(uid: String): Flow<List<SeoPost>> =
        observeSubcollection(uid, "posts", SeoPost::class.java)

    fun observeReviews(uid: String): Flow<List<CustomerReview>> =
        observeSubcollection(uid, "reviews", CustomerReview::class.java)

    fun observeCompetitors(uid: String): Flow<List<CompetitorEntry>> =
        observeSubcollection(uid, "competitors", CompetitorEntry::class.java)

    fun observeKeywords(uid: String): Flow<List<KeywordTarget>> =
        observeSubcollection(uid, "keywords", KeywordTarget::class.java)

    fun observeTeamMembers(uid: String): Flow<List<TeamMember>> =
        observeSubcollection(uid, "team_members", TeamMember::class.java)

    fun observeNotifications(uid: String): Flow<List<SystemNotification>> =
        observeSubcollection(uid, "notifications", SystemNotification::class.java)

    fun observeAnalyticsLogs(uid: String): Flow<List<AnalyticsLog>> =
        observeSubcollection(uid, "analytics_logs", AnalyticsLog::class.java)

    private fun <T> observeSubcollection(
        uid: String,
        subcollection: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        requireUserId(uid)
        val colRef = userDoc(uid).collection(subcollection)
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        clazz,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, colRef.path, uid) }
        )
    }

    private fun handleStreamError(
        error: Throwable,
        op: OperationType,
        path: String,
        uid: String
    ): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    suspend fun ensureInitialProfile(uid: String, displayName: String?): Result<Unit> =
        try {
            requireUserId(uid)
            val docRef = userDoc(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val initial = BusinessProfile(
                    userId = uid,
                    ownerName = displayName?.take(80)?.ifBlank { "Business Owner" } ?: "Business Owner",
                    businessName = "My Local Business",
                    category = "Local Service",
                    description = "Trusted local business serving neighborhood customers.",
                    gmbStatus = "not_linked",
                    gmbCtaType = "CALL"
                )
                docRef.set(initial.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$uid")
            Result.failure(e)
        }

    suspend fun updateBusinessBranding(
        uid: String,
        businessName: String,
        category: String,
        ownerName: String,
        ownerPhone: String,
        address: String,
        websiteUrl: String,
        description: String,
        ownerPhotoPath: String,
        serviceLogoPath: String
    ): Result<Unit> = try {
        requireUserId(uid)
        require(businessName.trim().isNotEmpty()) { "Business name is required" }
        val docRef = userDoc(uid)
        docRef.update(
            mapOf(
                "businessName" to businessName.trim().take(100),
                "category" to category.trim().take(80).ifBlank { "Local Service" },
                "ownerName" to ownerName.trim().take(80).ifBlank { "Business Owner" },
                "ownerPhone" to ownerPhone.trim().take(30),
                "address" to address.trim().take(200),
                "websiteUrl" to websiteUrl.trim().take(200),
                "description" to description.trim().take(1000),
                "ownerPhotoPath" to ownerPhotoPath.take(500),
                "serviceLogoPath" to serviceLogoPath.take(500),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        Result.failure(e)
    }

    suspend fun updateGmbAndChannels(
        uid: String,
        gmbStatus: String,
        gmbPlaceUrl: String,
        gmbCtaType: String,
        whatsappNumber: String,
        instagramHandle: String,
        facebookPage: String,
        autoReplyReviews: Boolean,
        dailyAutoPost: Boolean,
        monthlyWhatsappReport: Boolean
    ): Result<Unit> = try {
        requireUserId(uid)
        val validStatus = if (gmbStatus in listOf("not_linked", "creating", "linked")) gmbStatus else "not_linked"
        val validCta = if (gmbCtaType in listOf("CALL", "BOOK", "LEARN_MORE", "ORDER", "SIGN_UP")) gmbCtaType else "CALL"
        userDoc(uid).update(
            mapOf(
                "gmbStatus" to validStatus,
                "gmbPlaceUrl" to gmbPlaceUrl.trim().take(300),
                "gmbCtaType" to validCta,
                "whatsappNumber" to whatsappNumber.trim().take(30),
                "instagramHandle" to instagramHandle.trim().take(80),
                "facebookPage" to facebookPage.trim().take(80),
                "autoReplyReviews" to autoReplyReviews,
                "dailyAutoPost" to dailyAutoPost,
                "monthlyWhatsappReport" to monthlyWhatsappReport,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        Result.failure(e)
    }

    suspend fun updatePrivacyAndGdpr(
        uid: String,
        gdprConsent: Boolean,
        e2eeEnabled: Boolean
    ): Result<Unit> = try {
        requireUserId(uid)
        userDoc(uid).update(
            mapOf(
                "gdprConsent" to gdprConsent,
                "e2eeEnabled" to e2eeEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        Result.failure(e)
    }

    suspend fun updateSubscription(
        uid: String,
        tier: String,
        billingCycle: String,
        trialActive: Boolean,
        paymentMethodLabel: String
    ): Result<Unit> = try {
        requireUserId(uid)
        val safeTier = if (tier in listOf("TRIAL", "STARTER", "GROWTH", "ENTERPRISE")) tier else "TRIAL"
        val safeCycle = if (billingCycle in listOf("MONTHLY", "HALF_YEARLY", "ANNUAL")) billingCycle else "MONTHLY"
        userDoc(uid).update(
            mapOf(
                "subscriptionTier" to safeTier,
                "billingCycle" to safeCycle,
                "trialActive" to trialActive,
                "paymentMethodLabel" to paymentMethodLabel.trim().take(80),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        Result.failure(e)
    }

    suspend fun recordMetricEvent(
        uid: String,
        profile: BusinessProfile,
        addLeads: Long = 0L,
        addCalls: Long = 0L,
        addViews: Long = 0L
    ): Result<Unit> = try {
        requireUserId(uid)
        val newAllLeads = (profile.allTimeLeads + addLeads).coerceAtLeast(0L)
        val newAllCalls = (profile.allTimeCalls + addCalls).coerceAtLeast(0L)
        val newAllViews = (profile.allTimeViews + addViews).coerceAtLeast(0L)
        val newMonLeads = (profile.monthlyLeads + addLeads).coerceAtLeast(0L)
        val newMonCalls = (profile.monthlyCalls + addCalls).coerceAtLeast(0L)
        val newMonViews = (profile.monthlyViews + addViews).coerceAtLeast(0L)

        userDoc(uid).update(
            mapOf(
                "allTimeLeads" to newAllLeads,
                "allTimeCalls" to newAllCalls,
                "allTimeViews" to newAllViews,
                "monthlyLeads" to newMonLeads,
                "monthlyCalls" to newMonCalls,
                "monthlyViews" to newMonViews,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val monthDocId = SimpleDateFormat("yyyy_MM", Locale.US).format(Date())
        val monthLabel = SimpleDateFormat("MMM yyyy", Locale.US).format(Date())
        val logRef = userDoc(uid).collection("analytics_logs").document(monthDocId)
        val logSnap = logRef.get().await()
        if (logSnap.exists()) {
            logRef.update(
                mapOf(
                    "leads" to newMonLeads,
                    "calls" to newMonCalls,
                    "views" to newMonViews,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val log = AnalyticsLog(
                userId = uid,
                monthLabel = monthLabel,
                leads = newMonLeads,
                calls = newMonCalls,
                views = newMonViews
            )
            logRef.set(log.toCreateMap(uid)).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$uid/analytics_logs")
        Result.failure(e)
    }

    suspend fun createPost(uid: String, post: SeoPost): Result<String> = try {
        requireUserId(uid)
        require(post.title.trim().isNotEmpty()) { "Post title cannot be empty" }
        val docRef = userDoc(uid).collection("posts").document()
        docRef.set(post.toCreateMap(uid)).await()
        Result.success(docRef.id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$uid/posts")
        Result.failure(e)
    }

    suspend fun updatePostInModal(uid: String, post: SeoPost): Result<Unit> = try {
        requireUserId(uid)
        require(post.id.isNotBlank()) { "Post ID required" }
        userDoc(uid).collection("posts").document(post.id).update(
            mapOf(
                "title" to post.title.trim().take(120).ifBlank { "Local Service Update" },
                "description" to post.description.trim().take(1200).ifBlank { "Local service update." },
                "workPerformedBy" to post.workPerformedBy.trim().take(80).ifBlank { "Business Team" },
                "channels" to post.channels.take(5).ifEmpty { listOf("GMB") },
                "status" to (if (post.status in listOf("DRAFT", "SCHEDULED", "PUBLISHED")) post.status else "SCHEDULED"),
                "scheduledDateLabel" to post.scheduledDateLabel.take(40),
                "ctaLabel" to post.ctaLabel.take(40),
                "posterAccentHex" to post.posterAccentHex.take(16).ifBlank { "#047857" },
                "posterImagePath" to post.posterImagePath.take(500),
                "ownerPhotoSnapshotPath" to post.ownerPhotoSnapshotPath.take(500),
                "serviceLogoSnapshotPath" to post.serviceLogoSnapshotPath.take(500),
                "ownerPhoneSnapshot" to post.ownerPhoneSnapshot.take(30),
                "addressSnapshot" to post.addressSnapshot.take(200),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid/posts/${post.id}")
        Result.failure(e)
    }

    suspend fun publishPost(uid: String, postId: String, currentViews: Long, currentLeads: Long): Result<Unit> = try {
        requireUserId(uid)
        userDoc(uid).collection("posts").document(postId).update(
            mapOf(
                "status" to "PUBLISHED",
                "viewsCount" to currentViews,
                "leadsCount" to currentLeads,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid/posts/$postId")
        Result.failure(e)
    }

    suspend fun deletePost(uid: String, postId: String): Result<Unit> = try {
        requireUserId(uid)
        userDoc(uid).collection("posts").document(postId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/$uid/posts/$postId")
        Result.failure(e)
    }

    suspend fun addReview(uid: String, review: CustomerReview): Result<String> = try {
        requireUserId(uid)
        val docRef = userDoc(uid).collection("reviews").document()
        docRef.set(review.toCreateMap(uid)).await()
        Result.success(docRef.id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$uid/reviews")
        Result.failure(e)
    }

    suspend fun replyToReview(
        uid: String,
        reviewId: String,
        replyText: String,
        autoReplied: Boolean
    ): Result<Unit> = try {
        requireUserId(uid)
        require(replyText.trim().isNotEmpty()) { "Reply cannot be empty" }
        userDoc(uid).collection("reviews").document(reviewId).update(
            mapOf(
                "replyText" to replyText.trim().take(1000),
                "replied" to true,
                "autoReplied" to autoReplied,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid/reviews/$reviewId")
        Result.failure(e)
    }

    suspend fun addCompetitor(uid: String, entry: CompetitorEntry): Result<Unit> = try {
        requireUserId(uid)
        val docRef = userDoc(uid).collection("competitors").document()
        docRef.set(entry.toCreateMap(uid)).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$uid/competitors")
        Result.failure(e)
    }

    suspend fun updateCompetitorAnalysis(
        uid: String,
        competitor: CompetitorEntry,
        aiGapSummary: String
    ): Result<Unit> = try {
        requireUserId(uid)
        userDoc(uid).collection("competitors").document(competitor.id).update(
            mapOf(
                "name" to competitor.name.take(100),
                "website" to competitor.website.take(200),
                "localRank" to competitor.localRank.coerceIn(1L, 100L),
                "rating" to competitor.rating.coerceIn(0.0, 5.0),
                "reviewCount" to competitor.reviewCount.coerceAtLeast(0L),
                "monthlyViewsEst" to competitor.monthlyViewsEst.coerceAtLeast(0L),
                "seoScore" to competitor.seoScore.coerceIn(0L, 100L),
                "aiGapSummary" to aiGapSummary.take(2000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid/competitors/${competitor.id}")
        Result.failure(e)
    }

    suspend fun deleteCompetitor(uid: String, competitorId: String): Result<Unit> = try {
        requireUserId(uid)
        userDoc(uid).collection("competitors").document(competitorId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/$uid/competitors/$competitorId")
        Result.failure(e)
    }

    suspend fun addKeyword(uid: String, keywordTarget: KeywordTarget): Result<Unit> = try {
        requireUserId(uid)
        val docRef = userDoc(uid).collection("keywords").document()
        docRef.set(keywordTarget.toCreateMap(uid)).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$uid/keywords")
        Result.failure(e)
    }

    suspend fun addTeamMember(uid: String, member: TeamMember): Result<Unit> = try {
        requireUserId(uid)
        val docRef = userDoc(uid).collection("team_members").document()
        docRef.set(member.toCreateMap(uid)).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$uid/team_members")
        Result.failure(e)
    }

    suspend fun updateTeamMemberRole(uid: String, memberId: String, newRole: String): Result<Unit> = try {
        requireUserId(uid)
        val safeRole = if (newRole in listOf("MANAGER", "EDITOR", "VIEWER")) newRole else "EDITOR"
        userDoc(uid).collection("team_members").document(memberId).update(
            mapOf(
                "memberRole" to safeRole,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid/team_members/$memberId")
        Result.failure(e)
    }

    suspend fun removeTeamMember(uid: String, memberId: String): Result<Unit> = try {
        requireUserId(uid)
        userDoc(uid).collection("team_members").document(memberId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/$uid/team_members/$memberId")
        Result.failure(e)
    }

    suspend fun createNotification(uid: String, title: String, message: String, severity: String = "INFO"): Result<Unit> = try {
        requireUserId(uid)
        val notif = SystemNotification(
            userId = uid,
            title = title,
            message = message,
            severity = severity,
            readFlag = false
        )
        userDoc(uid).collection("notifications").document().set(notif.toCreateMap(uid)).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$uid/notifications")
        Result.failure(e)
    }

    suspend fun markNotificationRead(uid: String, notificationId: String): Result<Unit> = try {
        requireUserId(uid)
        userDoc(uid).collection("notifications").document(notificationId).update(
            mapOf(
                "readFlag" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid/notifications/$notificationId")
        Result.failure(e)
    }

    suspend fun eraseUserWorkspaceGdpr(uid: String): Result<Unit> = try {
        requireUserId(uid)
        val subcollections = listOf(
            "posts", "reviews", "competitors", "keywords",
            "team_members", "notifications", "analytics_logs"
        )
        for (sub in subcollections) {
            val docs = userDoc(uid).collection(sub).get().await()
            for (doc in docs.documents) {
                doc.reference.delete().await()
            }
        }
        userDoc(uid).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/$uid")
        Result.failure(e)
    }

    companion object {
        private fun createConfiguredFirestore(context: Context): FirebaseFirestore {
            val appContext = context.applicationContext
            val databaseId = appContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            try {
                db.firestoreSettings = firestoreSettings {
                    setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                }
            } catch (e: IllegalStateException) {
                Log.d("SeoRepository", "Firestore persistent cache already configured.")
            }
            return db
        }
    }
}
