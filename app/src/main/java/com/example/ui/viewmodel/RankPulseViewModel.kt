package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AppLanguage
import com.example.core.EncryptionHelper
import com.example.core.NetworkMonitor
import com.example.core.PreferencesRepository
import com.example.core.ThemeColorPalette
import com.example.core.ThemePreference
import com.example.core.UserPreferences
import com.example.core.WorkspaceRole
import com.example.data.AiBrandIdentityResult
import com.example.data.AiKeywordSuggestion
import com.example.data.AiSeoRepository
import com.example.data.SeoRepository
import com.example.export.MediaAndExportService
import com.example.export.ReportExporter
import com.example.model.AnalyticsLog
import com.example.model.BusinessProfile
import com.example.model.CompetitorEntry
import com.example.model.CustomerReview
import com.example.model.KeywordTarget
import com.example.model.SeoPost
import com.example.model.SystemNotification
import com.example.model.TeamMember
import com.google.firebase.ai.Chat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "RankPulseVM"
private const val STOP_TIMEOUT_MILLIS = 5000L

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiResultState<out T> {
    data object Idle : AiResultState<Nothing>
    data object Loading : AiResultState<Nothing>
    data class Streaming(val text: String) : AiResultState<Nothing>
    data class Success<T>(val data: T) : AiResultState<T>
    data class Error(val message: String) : AiResultState<Nothing>
}

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String
)

sealed interface UiEvent {
    data class ShowMessage(val text: String) : UiEvent
    data class ShareUri(
        val uri: Uri,
        val mimeType: String,
        val text: String,
        val targetPackage: String? = null
    ) : UiEvent
}

class RankPulseViewModel(
    private val seoRepo: SeoRepository,
    private val aiRepo: AiSeoRepository,
    private val prefsRepo: PreferencesRepository,
    private val mediaService: MediaAndExportService,
    networkMonitor: NetworkMonitor,
    private val currentUserId: String,
    private val userDisplayName: String?
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UserPreferences())

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), true)

    val profileState: StateFlow<UiState<BusinessProfile>> = seoRepo.observeBusinessProfile(currentUserId)
        .map<BusinessProfile?, UiState<BusinessProfile>> { profile ->
            UiState.Success(profile ?: BusinessProfile(userId = currentUserId, ownerName = userDisplayName ?: "Business Owner"))
        }
        .unwrapFirestoreErrors("profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val postsState: StateFlow<UiState<List<SeoPost>>> =
        seoRepo.observePosts(currentUserId).toUiState("posts")

    val reviewsState: StateFlow<UiState<List<CustomerReview>>> =
        seoRepo.observeReviews(currentUserId).toUiState("reviews")

    val competitorsState: StateFlow<UiState<List<CompetitorEntry>>> =
        seoRepo.observeCompetitors(currentUserId).toUiState("competitors")

    val keywordsState: StateFlow<UiState<List<KeywordTarget>>> =
        seoRepo.observeKeywords(currentUserId).toUiState("keywords")

    val teamMembersState: StateFlow<UiState<List<TeamMember>>> =
        seoRepo.observeTeamMembers(currentUserId).toUiState("team_members")

    val notificationsState: StateFlow<UiState<List<SystemNotification>>> =
        seoRepo.observeNotifications(currentUserId).toUiState("notifications")

    val analyticsLogsState: StateFlow<UiState<List<AnalyticsLog>>> =
        seoRepo.observeAnalyticsLogs(currentUserId).toUiState("analytics_logs")

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    // AI States
    private val _copyOptimizerState = MutableStateFlow<AiResultState<String>>(AiResultState.Idle)
    val copyOptimizerState: StateFlow<AiResultState<String>> = _copyOptimizerState.asStateFlow()

    private val _keywordSuggestionsState = MutableStateFlow<AiResultState<List<AiKeywordSuggestion>>>(AiResultState.Idle)
    val keywordSuggestionsState: StateFlow<AiResultState<List<AiKeywordSuggestion>>> = _keywordSuggestionsState.asStateFlow()

    private val _brandKitState = MutableStateFlow<AiResultState<AiBrandIdentityResult>>(AiResultState.Idle)
    val brandKitState: StateFlow<AiResultState<AiBrandIdentityResult>> = _brandKitState.asStateFlow()

    private val _aiLogoImageState = MutableStateFlow<AiResultState<String>>(AiResultState.Idle)
    val aiLogoImageState: StateFlow<AiResultState<String>> = _aiLogoImageState.asStateFlow()

    private val _aiPostImageState = MutableStateFlow<AiResultState<String>>(AiResultState.Idle)
    val aiPostImageState: StateFlow<AiResultState<String>> = _aiPostImageState.asStateFlow()

    private val _aiPostCopyState = MutableStateFlow<AiResultState<Pair<String, String>>>(AiResultState.Idle)
    val aiPostCopyState: StateFlow<AiResultState<Pair<String, String>>> = _aiPostCopyState.asStateFlow()

    private val _reviewDrafts = MutableStateFlow<Map<String, String>>(emptyMap())
    val reviewDrafts: StateFlow<Map<String, String>> = _reviewDrafts.asStateFlow()

    private val _aiBusyReviewId = MutableStateFlow<String?>(null)
    val aiBusyReviewId: StateFlow<String?> = _aiBusyReviewId.asStateFlow()

    private val _aiBusyCompetitorId = MutableStateFlow<String?>(null)
    val aiBusyCompetitorId: StateFlow<String?> = _aiBusyCompetitorId.asStateFlow()

    // Chat State
    private var activeChat: Chat? = null
    private var chatLanguage: AppLanguage? = null
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    private val _chatStreaming = MutableStateFlow(false)
    val chatStreaming: StateFlow<Boolean> = _chatStreaming.asStateFlow()

    // Decrypted E2EE vault preview
    private val _decryptedVaultText = MutableStateFlow("")
    val decryptedVaultText: StateFlow<String> = _decryptedVaultText.asStateFlow()

    init {
        viewModelScope.launch {
            seoRepo.ensureInitialProfile(currentUserId, userDisplayName)
        }
        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                if (prefs.encryptedVaultNote.isNotBlank()) {
                    try {
                        val plain = withContext(Dispatchers.IO) {
                            EncryptionHelper.decrypt(prefs.encryptedVaultNote)
                        }
                        _decryptedVaultText.value = plain
                    } catch (e: Exception) {
                        Log.w(TAG, "Unable to decrypt local vault note", e)
                    }
                }
            }
        }
    }

    private fun <T> Flow<List<T>>.toUiState(label: String): StateFlow<UiState<List<T>>> =
        this.map<List<T>, UiState<List<T>>> { UiState.Success(it) }
            .unwrapFirestoreErrors(label)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private fun <T> Flow<UiState<T>>.unwrapFirestoreErrors(label: String): Flow<UiState<T>> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing $label", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load $label"))
        }

    private fun activeRole(): WorkspaceRole = preferences.value.activePreviewRole

    private suspend fun guardRole(allowed: Boolean): Boolean {
        if (!allowed) {
            _events.emit(UiEvent.ShowMessage("Permission denied for ${activeRole().label} role."))
            return false
        }
        return true
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { prefsRepo.setTheme(theme) }
    }

    fun setColorPalette(palette: ThemeColorPalette) {
        viewModelScope.launch { prefsRepo.setColorPalette(palette) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefsRepo.setLanguage(language)
            activeChat = null
        }
    }

    fun dismissFirstLoginPopup() {
        viewModelScope.launch { prefsRepo.markOnboardingSeen(currentUserId) }
    }

    fun setPreviewRole(role: WorkspaceRole) {
        viewModelScope.launch { prefsRepo.setPreviewRole(role) }
    }

    fun saveEncryptedVaultNote(plainNote: String) {
        viewModelScope.launch {
            try {
                val cipher = withContext(Dispatchers.IO) {
                    EncryptionHelper.encrypt(plainNote)
                }
                prefsRepo.saveEncryptedVaultNote(cipher)
                _decryptedVaultText.value = plainNote
                _events.emit(UiEvent.ShowMessage("Encrypted & stored with AES-256 Android Keystore."))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.localizedMessage ?: "Encryption failed"))
            }
        }
    }

    fun updateBusinessProfile(
        businessName: String,
        category: String,
        ownerName: String,
        ownerPhone: String,
        address: String,
        websiteUrl: String,
        description: String,
        ownerPhotoPath: String,
        serviceLogoPath: String
    ) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canEditContent)) return@launch
            val res = seoRepo.updateBusinessBranding(
                uid = currentUserId,
                businessName = businessName,
                category = category,
                ownerName = ownerName,
                ownerPhone = ownerPhone,
                address = address,
                websiteUrl = websiteUrl,
                description = description,
                ownerPhotoPath = ownerPhotoPath,
                serviceLogoPath = serviceLogoPath
            )
            if (res.isSuccess) {
                _events.emit(UiEvent.ShowMessage("Business profile & branding saved."))
            } else {
                _events.emit(UiEvent.ShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed to save profile"))
            }
        }
    }

    fun uploadOwnerPhoto(uri: Uri, currentProfile: BusinessProfile, alsoUpdatePost: SeoPost? = null) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canEditContent)) return@launch
            try {
                val path = mediaService.persistPickedMedia(uri, "owner_photo")
                seoRepo.updateBusinessBranding(
                    uid = currentUserId,
                    businessName = currentProfile.businessName,
                    category = currentProfile.category,
                    ownerName = currentProfile.ownerName,
                    ownerPhone = currentProfile.ownerPhone,
                    address = currentProfile.address,
                    websiteUrl = currentProfile.websiteUrl,
                    description = currentProfile.description,
                    ownerPhotoPath = path,
                    serviceLogoPath = currentProfile.serviceLogoPath
                )
                if (alsoUpdatePost != null && alsoUpdatePost.id.isNotBlank()) {
                    seoRepo.updatePostInModal(
                        currentUserId,
                        alsoUpdatePost.copy(ownerPhotoSnapshotPath = path)
                    )
                }
                _events.emit(UiEvent.ShowMessage("Owner photo updated across profile & posters."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.localizedMessage ?: "Failed to save owner photo"))
            }
        }
    }

    fun uploadServiceLogo(uri: Uri, currentProfile: BusinessProfile, alsoUpdatePost: SeoPost? = null) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canEditContent)) return@launch
            try {
                val path = mediaService.persistPickedMedia(uri, "service_logo")
                seoRepo.updateBusinessBranding(
                    uid = currentUserId,
                    businessName = currentProfile.businessName,
                    category = currentProfile.category,
                    ownerName = currentProfile.ownerName,
                    ownerPhone = currentProfile.ownerPhone,
                    address = currentProfile.address,
                    websiteUrl = currentProfile.websiteUrl,
                    description = currentProfile.description,
                    ownerPhotoPath = currentProfile.ownerPhotoPath,
                    serviceLogoPath = path
                )
                if (alsoUpdatePost != null && alsoUpdatePost.id.isNotBlank()) {
                    seoRepo.updatePostInModal(
                        currentUserId,
                        alsoUpdatePost.copy(serviceLogoSnapshotPath = path)
                    )
                }
                _events.emit(UiEvent.ShowMessage("Service logo updated."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.localizedMessage ?: "Failed to save logo"))
            }
        }
    }

    fun updateGmbAndChannels(
        gmbStatus: String,
        gmbPlaceUrl: String,
        gmbCtaType: String,
        whatsappNumber: String,
        instagramHandle: String,
        facebookPage: String,
        autoReplyReviews: Boolean,
        dailyAutoPost: Boolean,
        monthlyWhatsappReport: Boolean
    ) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canReplyOrAnalyze)) return@launch
            val res = seoRepo.updateGmbAndChannels(
                uid = currentUserId,
                gmbStatus = gmbStatus,
                gmbPlaceUrl = gmbPlaceUrl,
                gmbCtaType = gmbCtaType,
                whatsappNumber = whatsappNumber,
                instagramHandle = instagramHandle,
                facebookPage = facebookPage,
                autoReplyReviews = autoReplyReviews,
                dailyAutoPost = dailyAutoPost,
                monthlyWhatsappReport = monthlyWhatsappReport
            )
            if (res.isSuccess) {
                seoRepo.createNotification(
                    uid = currentUserId,
                    title = "Google & Channel Sync Updated",
                    message = "GMB status: ${gmbStatus.uppercase()} • CTA: $gmbCtaType",
                    severity = "INFO"
                )
                mediaService.notifySystemAlert(
                    "RankPulse Sync",
                    "Google Business Profile & channels updated.",
                    1001
                )
                _events.emit(UiEvent.ShowMessage("GMB & social channels updated."))
            } else {
                _events.emit(UiEvent.ShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Update failed"))
            }
        }
    }

    fun recordLeadCallView(profile: BusinessProfile, leads: Long, calls: Long, views: Long) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canEditContent)) return@launch
            val res = seoRepo.recordMetricEvent(currentUserId, profile, leads, calls, views)
            if (res.isFailure) {
                _events.emit(UiEvent.ShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Metric update failed"))
            }
        }
    }

    fun createBrandedPost(
        profile: BusinessProfile,
        title: String,
        description: String,
        workPerformedBy: String,
        channels: List<String>,
        status: String,
        scheduledDateLabel: String,
        ctaLabel: String,
        posterImagePath: String,
        downloadOnPublish: Boolean = false
    ) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canEditContent)) return@launch
            val post = SeoPost(
                userId = currentUserId,
                title = title.trim().ifBlank { "${profile.businessName} Service Update" },
                description = description.trim().ifBlank { profile.description.ifBlank { "Trusted local service." } },
                workPerformedBy = workPerformedBy.trim().ifBlank { profile.ownerName },
                channels = channels.ifEmpty { listOf("WHATSAPP", "INSTAGRAM", "FACEBOOK", "GMB", "WEBSITE") },
                status = status,
                scheduledDateLabel = scheduledDateLabel,
                ctaLabel = ctaLabel.ifBlank { profile.gmbCtaType },
                posterImagePath = posterImagePath,
                // Mandatory snapshot of owner photo, service logo, phone & address on every post
                ownerPhotoSnapshotPath = profile.ownerPhotoPath,
                serviceLogoSnapshotPath = profile.serviceLogoPath,
                ownerPhoneSnapshot = profile.ownerPhone,
                addressSnapshot = profile.address
            )
            val res = seoRepo.createPost(currentUserId, post)
            if (res.isSuccess) {
                seoRepo.createNotification(
                    uid = currentUserId,
                    title = if (status == "PUBLISHED") "Post Published" else "Daily Post Scheduled",
                    message = "${post.title} queued for ${post.channels.joinToString(", ")}",
                    severity = "INFO"
                )
                if (status == "PUBLISHED" && downloadOnPublish) {
                    try {
                        val savedLocation = mediaService.downloadPosterToDevice(post, profile)
                        _events.emit(UiEvent.ShowMessage("Post published & poster downloaded: $savedLocation"))
                    } catch (e: Exception) {
                        _events.emit(UiEvent.ShowMessage("Post published, download failed: ${e.localizedMessage}"))
                    }
                } else {
                    _events.emit(UiEvent.ShowMessage("Branded poster saved with Owner Photo, Logo, Phone & Address."))
                }
            } else {
                _events.emit(UiEvent.ShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed to create post"))
            }
        }
    }

    fun updatePostInModal(post: SeoPost) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canEditContent)) return@launch
            val res = seoRepo.updatePostInModal(currentUserId, post)
            if (res.isSuccess) {
                _events.emit(UiEvent.ShowMessage("Post updated."))
            } else {
                _events.emit(UiEvent.ShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Update failed"))
            }
        }
    }

    fun publishPost(post: SeoPost, profile: BusinessProfile? = null, downloadAfterPublish: Boolean = false) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canEditContent)) return@launch
            val res = seoRepo.publishPost(currentUserId, post.id, post.viewsCount + 1, post.leadsCount)
            if (res.isSuccess) {
                if (downloadAfterPublish && profile != null) {
                    try {
                        val savedLocation = mediaService.downloadPosterToDevice(post.copy(status = "PUBLISHED"), profile)
                        _events.emit(UiEvent.ShowMessage("Post published & downloaded to $savedLocation"))
                    } catch (e: Exception) {
                        _events.emit(UiEvent.ShowMessage("Published, but poster download failed: ${e.localizedMessage}"))
                    }
                } else {
                    _events.emit(UiEvent.ShowMessage("Post marked Published across channels."))
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canReplyOrAnalyze)) return@launch
            val res = seoRepo.deletePost(currentUserId, postId)
            if (res.isSuccess) {
                _events.emit(UiEvent.ShowMessage("Post deleted."))
            }
        }
    }

    fun sharePostPoster(post: SeoPost, profile: BusinessProfile, targetPackage: String? = null) {
        viewModelScope.launch {
            try {
                val uri = mediaService.renderPosterUri(post, profile)
                val caption = "${post.title}\n${post.description}\nCall: ${profile.ownerPhone} | ${profile.address}"
                _events.emit(UiEvent.ShareUri(uri, "image/png", caption, targetPackage))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.localizedMessage ?: "Poster render failed"))
            }
        }
    }

    fun downloadPostPoster(post: SeoPost, profile: BusinessProfile) {
        viewModelScope.launch {
            try {
                val savedLocation = mediaService.downloadPosterToDevice(post, profile)
                _events.emit(UiEvent.ShowMessage("Poster saved to $savedLocation"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.localizedMessage ?: "Poster download failed"))
            }
        }
    }

    fun exportPdfReport(
        profile: BusinessProfile,
        logs: List<AnalyticsLog>,
        keywords: List<KeywordTarget>,
        competitors: List<CompetitorEntry>,
        posts: List<SeoPost>
    ) {
        viewModelScope.launch {
            try {
                val uri = mediaService.exportPdfUri(profile, logs, keywords, competitors, posts)
                _events.emit(UiEvent.ShareUri(uri, "application/pdf", "${profile.businessName} SEO Report (PDF)"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.localizedMessage ?: "PDF export failed"))
            }
        }
    }

    fun exportCsvReport(profile: BusinessProfile, logs: List<AnalyticsLog>, keywords: List<KeywordTarget>) {
        viewModelScope.launch {
            try {
                val uri = mediaService.exportCsvUri(profile, logs, keywords)
                _events.emit(UiEvent.ShareUri(uri, "text/csv", "${profile.businessName} Analytics CSV"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.localizedMessage ?: "CSV export failed"))
            }
        }
    }

    fun sendMonthlyWhatsappReport(
        profile: BusinessProfile,
        logs: List<AnalyticsLog>,
        keywords: List<KeywordTarget>,
        competitors: List<CompetitorEntry>,
        posts: List<SeoPost>
    ) {
        viewModelScope.launch {
            try {
                val uri = mediaService.exportPdfUri(profile, logs, keywords, competitors, posts)
                val summary = ReportExporter.buildMonthlyWhatsappSummary(profile, preferences.value.language)
                _events.emit(UiEvent.ShareUri(uri, "application/pdf", summary, "com.whatsapp"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.localizedMessage ?: "WhatsApp report failed"))
            }
        }
    }

    // --- AI Actions ---
    fun optimizeWebsiteCopy(profile: BusinessProfile, pageTopic: String, existingCopy: String) {
        viewModelScope.launch {
            _copyOptimizerState.value = AiResultState.Loading
            try {
                val out = aiRepo.optimizeWebsiteCopy(profile, pageTopic, existingCopy, preferences.value.language)
                _copyOptimizerState.value = AiResultState.Success(out)
            } catch (e: TimeoutCancellationException) {
                _copyOptimizerState.value = AiResultState.Error("Request timed out after 60s. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Copy optimizer failed", e)
                _copyOptimizerState.value = AiResultState.Error(e.localizedMessage ?: "AI optimization failed.")
            }
        }
    }

    fun generateKeywords(profile: BusinessProfile, seedTopic: String) {
        viewModelScope.launch {
            _keywordSuggestionsState.value = AiResultState.Loading
            try {
                val list = aiRepo.generateKeywordRecommendations(profile, seedTopic, preferences.value.language)
                _keywordSuggestionsState.value = AiResultState.Success(list)
            } catch (e: TimeoutCancellationException) {
                _keywordSuggestionsState.value = AiResultState.Error("Keyword generation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Keyword finder failed", e)
                _keywordSuggestionsState.value = AiResultState.Error(e.localizedMessage ?: "Keyword AI failed.")
            }
        }
    }

    fun saveKeywordToTracker(suggestion: AiKeywordSuggestion) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canEditContent)) return@launch
            val kw = KeywordTarget(
                userId = currentUserId,
                keyword = suggestion.keyword,
                intent = suggestion.intent,
                difficulty = suggestion.difficulty,
                currentRank = 12L,
                aiTip = suggestion.aiTip
            )
            val res = seoRepo.addKeyword(currentUserId, kw)
            if (res.isSuccess) {
                _events.emit(UiEvent.ShowMessage("Saved '${suggestion.keyword}' to tracker."))
            }
        }
    }

    fun generateBrandIdentity(profile: BusinessProfile) {
        viewModelScope.launch {
            _brandKitState.value = AiResultState.Loading
            try {
                val kit = aiRepo.generateBrandDescriptionAndCta(profile, preferences.value.language)
                _brandKitState.value = AiResultState.Success(kit)
            } catch (e: TimeoutCancellationException) {
                _brandKitState.value = AiResultState.Error("Brand kit generation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Brand kit failed", e)
                _brandKitState.value = AiResultState.Error(e.localizedMessage ?: "Brand AI failed.")
            }
        }
    }

    fun generateAiServiceLogo(profile: BusinessProfile) {
        viewModelScope.launch {
            _aiLogoImageState.value = AiResultState.Loading
            try {
                val prompt = "Clean minimalist vector business emblem logo for ${profile.businessName} (${profile.category}), emerald and amber geometric symbol on solid crisp white background, modern brand mark, no small text"
                val path = mediaService.generateAndSaveAiImage(aiRepo, prompt, "ai_logo")
                _aiLogoImageState.value = AiResultState.Success(path)
                seoRepo.updateBusinessBranding(
                    uid = currentUserId,
                    businessName = profile.businessName,
                    category = profile.category,
                    ownerName = profile.ownerName,
                    ownerPhone = profile.ownerPhone,
                    address = profile.address,
                    websiteUrl = profile.websiteUrl,
                    description = profile.description,
                    ownerPhotoPath = profile.ownerPhotoPath,
                    serviceLogoPath = path
                )
                _events.emit(UiEvent.ShowMessage("AI logo generated & applied to your brand profile!"))
            } catch (e: TimeoutCancellationException) {
                _aiLogoImageState.value = AiResultState.Error("Logo generation timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI logo image failed", e)
                _aiLogoImageState.value = AiResultState.Error(e.localizedMessage ?: "AI logo generation failed.")
            }
        }
    }

    fun generateAiPostSceneImage(profile: BusinessProfile, topic: String) {
        viewModelScope.launch {
            _aiPostImageState.value = AiResultState.Loading
            try {
                val prompt = "High-quality editorial commercial photo illustration representing '$topic' for ${profile.businessName} (${profile.category}), bright modern local business aesthetic, no text overlays"
                val path = mediaService.generateAndSaveAiImage(aiRepo, prompt, "ai_post_scene")
                _aiPostImageState.value = AiResultState.Success(path)
            } catch (e: TimeoutCancellationException) {
                _aiPostImageState.value = AiResultState.Error("Poster artwork generation timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI post image failed", e)
                _aiPostImageState.value = AiResultState.Error(e.localizedMessage ?: "AI poster artwork failed.")
            }
        }
    }

    fun generateAiPostCopy(profile: BusinessProfile, topic: String, workBy: String) {
        viewModelScope.launch {
            _aiPostCopyState.value = AiResultState.Loading
            try {
                val copy = aiRepo.generateDailyPostCopy(profile, topic, workBy, preferences.value.language)
                _aiPostCopyState.value = AiResultState.Success(copy)
            } catch (e: TimeoutCancellationException) {
                _aiPostCopyState.value = AiResultState.Error("Post copy generation timed out.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI post copy failed", e)
                _aiPostCopyState.value = AiResultState.Error(e.localizedMessage ?: "AI post copy failed.")
            }
        }
    }

    fun sendChatMessage(profile: BusinessProfile, message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty() || _chatStreaming.value) return
        val lang = preferences.value.language
        if (activeChat == null || chatLanguage != lang) {
            activeChat = aiRepo.startSupportChat(profile, lang)
            chatLanguage = lang
        }
        val chat = activeChat ?: return

        val userMsg = ChatMessage("u_${System.currentTimeMillis()}", true, trimmed)
        val botMsgId = "b_${System.currentTimeMillis()}"
        _chatMessages.value = _chatMessages.value + userMsg + ChatMessage(botMsgId, false, "...")
        _chatStreaming.value = true

        viewModelScope.launch {
            try {
                aiRepo.sendChatMessageStream(chat, trimmed).collect { partial ->
                    _chatMessages.value = _chatMessages.value.map {
                        if (it.id == botMsgId) it.copy(text = partial) else it
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _chatMessages.value = _chatMessages.value.map {
                    if (it.id == botMsgId) it.copy(text = "Timed out after 60s. Please retry.") else it
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat stream failed", e)
                _chatMessages.value = _chatMessages.value.map {
                    if (it.id == botMsgId) it.copy(text = "Error: ${e.localizedMessage ?: "AI chat failed"}") else it
                }
            } finally {
                _chatStreaming.value = false
            }
        }
    }

    // --- Reviews & Auto-Reply ---
    fun addCustomerReview(profile: BusinessProfile, reviewerName: String, rating: Long, comment: String) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canEditContent)) return@launch
            val review = CustomerReview(
                userId = currentUserId,
                reviewerName = reviewerName,
                rating = rating,
                comment = comment
            )
            val res = seoRepo.addReview(currentUserId, review)
            if (res.isSuccess) {
                val reviewId = res.getOrNull().orEmpty()
                if (profile.autoReplyReviews && reviewId.isNotBlank()) {
                    generateAndSendReviewReply(profile, review.copy(id = reviewId), autoPublish = true)
                } else {
                    seoRepo.createNotification(
                        uid = currentUserId,
                        title = "New ${rating}★ Review",
                        message = "$reviewerName left a review: ${comment.take(80)}",
                        severity = if (rating <= 2L) "CRITICAL" else "INFO"
                    )
                }
            }
        }
    }

    fun generateAndSendReviewReply(profile: BusinessProfile, review: CustomerReview, autoPublish: Boolean) {
        viewModelScope.launch {
            _aiBusyReviewId.value = review.id
            try {
                val reply = aiRepo.generateReviewReply(profile, review, preferences.value.language)
                if (autoPublish) {
                    seoRepo.replyToReview(currentUserId, review.id, reply, autoReplied = true)
                    _events.emit(UiEvent.ShowMessage("AI auto-replied to ${review.reviewerName}."))
                } else {
                    _reviewDrafts.value = _reviewDrafts.value + (review.id to reply)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.localizedMessage ?: "AI reply failed"))
            } finally {
                _aiBusyReviewId.value = null
            }
        }
    }

    fun publishReviewReply(reviewId: String, replyText: String) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canReplyOrAnalyze)) return@launch
            val res = seoRepo.replyToReview(currentUserId, reviewId, replyText, autoReplied = false)
            if (res.isSuccess) {
                _reviewDrafts.value = _reviewDrafts.value - reviewId
                _events.emit(UiEvent.ShowMessage("Review reply saved."))
            }
        }
    }

    // --- Competitors ---
    fun addCompetitor(
        name: String,
        website: String,
        rank: Long,
        rating: Double,
        reviews: Long,
        monthlyViewsEst: Long = 0L,
        seoScore: Long = 50L
    ) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canEditContent)) return@launch
            val entry = CompetitorEntry(
                userId = currentUserId,
                name = name,
                website = website,
                localRank = rank,
                rating = rating,
                reviewCount = reviews,
                monthlyViewsEst = monthlyViewsEst.coerceAtLeast(0L),
                seoScore = seoScore.coerceIn(0L, 100L)
            )
            val res = seoRepo.addCompetitor(currentUserId, entry)
            if (res.isSuccess) _events.emit(UiEvent.ShowMessage("Competitor tracked on analytics dashboard."))
            else _events.emit(UiEvent.ShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Failed to add competitor"))
        }
    }

    fun analyzeCompetitorWithAi(
        profile: BusinessProfile,
        competitor: CompetitorEntry,
        reviews: List<CustomerReview> = emptyList(),
        keywords: List<KeywordTarget> = emptyList(),
        posts: List<SeoPost> = emptyList()
    ) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canReplyOrAnalyze)) return@launch
            _aiBusyCompetitorId.value = competitor.id
            try {
                val userRank = keywords.filter { it.currentRank > 0L }.minOfOrNull { it.currentRank }
                    ?: if (profile.gmbStatus == "linked") 4L else 15L
                val userAvgRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0
                val userReviewCount = reviews.size.toLong()
                val userSeoScore = computeUserSeoScore(profile, reviews, keywords, posts)
                val analysis = aiRepo.analyzeCompetitorGap(
                    profile = profile,
                    competitor = competitor,
                    userLocalRank = userRank,
                    userAvgRating = userAvgRating,
                    userReviewCount = userReviewCount,
                    userSeoScore = userSeoScore,
                    language = preferences.value.language
                )
                val saveRes = seoRepo.updateCompetitorAnalysis(currentUserId, competitor, analysis)
                if (saveRes.isSuccess) {
                    _events.emit(UiEvent.ShowMessage("AI gap analysis updated for ${competitor.name}."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.localizedMessage ?: "Competitor analysis failed"))
            } finally {
                _aiBusyCompetitorId.value = null
            }
        }
    }

    fun deleteCompetitor(competitorId: String) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canReplyOrAnalyze)) return@launch
            val res = seoRepo.deleteCompetitor(currentUserId, competitorId)
            if (res.isSuccess) {
                _events.emit(UiEvent.ShowMessage("Competitor removed."))
            } else {
                _events.emit(UiEvent.ShowMessage(res.exceptionOrNull()?.localizedMessage ?: "Delete failed"))
            }
        }
    }

    private fun computeUserSeoScore(
        profile: BusinessProfile,
        reviews: List<CustomerReview>,
        keywords: List<KeywordTarget>,
        posts: List<SeoPost>
    ): Long {
        var score = 15L
        when (profile.gmbStatus) {
            "linked" -> score += 25L
            "creating" -> score += 12L
        }
        if (profile.websiteUrl.isNotBlank()) score += 6L
        if (profile.description.isNotBlank()) score += 6L
        if (profile.ownerPhotoPath.isNotBlank() && profile.serviceLogoPath.isNotBlank()) score += 8L
        val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0
        score += ((reviews.size.coerceAtMost(10)) * 1L) + (avgRating * 2.0).toLong().coerceAtMost(10L)
        val top10Count = keywords.count { it.currentRank in 1L..10L }
        score += (keywords.size.coerceAtMost(5) * 1L) + (top10Count.coerceAtMost(5) * 2L)
        val publishedPosts = posts.count { it.status == "PUBLISHED" }
        score += (publishedPosts.coerceAtMost(5) * 2L)
        if (profile.monthlyViews >= 500L) score += 5L else if (profile.monthlyViews > 0L) score += 2L
        return score.coerceIn(0L, 100L)
    }

    // --- Subscription, RBAC, GDPR, Notifications ---
    fun updateSubscription(tier: String, billingCycle: String, trialActive: Boolean, paymentMethod: String) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canManageBillingAndTeam)) return@launch
            val res = seoRepo.updateSubscription(currentUserId, tier, billingCycle, trialActive, paymentMethod)
            if (res.isSuccess) {
                seoRepo.createNotification(
                    uid = currentUserId,
                    title = "Subscription Updated",
                    message = "$tier ($billingCycle) • $paymentMethod",
                    severity = "INFO"
                )
                _events.emit(UiEvent.ShowMessage("Plan updated to $tier ($billingCycle)."))
            }
        }
    }

    fun addTeamMember(name: String, email: String, role: String) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canManageBillingAndTeam)) return@launch
            val res = seoRepo.addTeamMember(
                currentUserId,
                TeamMember(userId = currentUserId, memberName = name, memberEmail = email, memberRole = role)
            )
            if (res.isSuccess) _events.emit(UiEvent.ShowMessage("Team member added as $role."))
        }
    }

    fun updateTeamMemberRole(memberId: String, newRole: String) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canManageBillingAndTeam)) return@launch
            seoRepo.updateTeamMemberRole(currentUserId, memberId, newRole)
        }
    }

    fun removeTeamMember(memberId: String) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canManageBillingAndTeam)) return@launch
            seoRepo.removeTeamMember(currentUserId, memberId)
        }
    }

    fun updatePrivacyAndGdpr(gdprConsent: Boolean, e2eeEnabled: Boolean) {
        viewModelScope.launch {
            if (!guardRole(activeRole().canManageBillingAndTeam)) return@launch
            seoRepo.updatePrivacyAndGdpr(currentUserId, gdprConsent, e2eeEnabled)
            _events.emit(UiEvent.ShowMessage("Privacy & GDPR preferences updated."))
        }
    }

    fun exportGdprData(profile: BusinessProfile, posts: List<SeoPost>, keywords: List<KeywordTarget>) {
        viewModelScope.launch {
            try {
                val uri = mediaService.exportGdprUri(profile, posts, keywords)
                _events.emit(UiEvent.ShareUri(uri, "application/json", "RankPulse GDPR Data Export"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.localizedMessage ?: "GDPR export failed"))
            }
        }
    }

    fun eraseWorkspaceGdpr() {
        viewModelScope.launch {
            if (!guardRole(activeRole().canManageBillingAndTeam)) return@launch
            val res = seoRepo.eraseUserWorkspaceGdpr(currentUserId)
            if (res.isSuccess) {
                seoRepo.ensureInitialProfile(currentUserId, userDisplayName)
                _events.emit(UiEvent.ShowMessage("Workspace data erased per GDPR right-to-erasure."))
            }
        }
    }

    fun markAllNotificationsRead(notifications: List<SystemNotification>) {
        viewModelScope.launch {
            notifications.filter { !it.readFlag }.forEach {
                seoRepo.markNotificationRead(currentUserId, it.id)
            }
        }
    }

    fun triggerCriticalSystemAlert(title: String, message: String) {
        viewModelScope.launch {
            seoRepo.createNotification(currentUserId, title, message, "CRITICAL")
            mediaService.notifySystemAlert(title, message, (System.currentTimeMillis() % 10000).toInt())
        }
    }
}
