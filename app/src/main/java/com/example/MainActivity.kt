package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.AppLanguage
import com.example.core.NetworkMonitor
import com.example.core.PreferencesRepository
import com.example.core.ThemeColorPalette
import com.example.core.ThemePreference
import com.example.core.configureAppCheck
import com.example.data.AiSeoRepository
import com.example.data.SeoRepository
import com.example.export.MediaAndExportService
import com.example.export.ReportExporter
import com.example.i18n.LocalStrings
import com.example.i18n.stringsFor
import com.example.model.BusinessProfile
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FirstLoginWelcomeDialog
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.NotificationsSheet
import com.example.ui.screens.SocialPostsScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.RankPulseTheme
import com.example.ui.viewmodel.RankPulseViewModel
import com.example.ui.viewmodel.UiEvent
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_SOCIAL = "social_posters"
const val ROUTE_AI_STUDIO = "ai_studio"
const val ROUTE_INSIGHTS = "insights"
const val ROUTE_WORKSPACE = "workspace"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // STEP 1: Mandatory App Check configuration with launch intent before Compose UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefsRepo = remember(context) { PreferencesRepository(context.applicationContext) }
            val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = com.example.core.UserPreferences()
            )
            val strings = remember(prefs.language) { stringsFor(prefs.language) }
            val scope = rememberCoroutineScope()

            CompositionLocalProvider(LocalStrings provides strings) {
                RankPulseTheme(
                    themePreference = prefs.theme,
                    colorPalette = prefs.colorPalette
                ) {
                    AppNavigation(
                        prefsRepo = prefsRepo,
                        currentLanguage = prefs.language,
                        onLanguageChange = { lang -> scope.launch { prefsRepo.setLanguage(lang) } }
                    )
                }
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    prefsRepo: PreferencesRepository,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            currentLanguage = currentLanguage,
            onLanguageChange = onLanguageChange,
            onAuthSuccess = { }
        )
    } else {
        key(user.uid) {
            val viewModel: RankPulseViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        RankPulseViewModel(
                            seoRepo = SeoRepository(app),
                            aiRepo = AiSeoRepository(),
                            prefsRepo = prefsRepo,
                            mediaService = MediaAndExportService(app),
                            networkMonitor = NetworkMonitor(app),
                            currentUserId = user.uid,
                            userDisplayName = user.displayName
                        )
                    }
                }
            )
            RankPulseAppShell(viewModel = viewModel, currentUserId = user.uid)
        }
    }
}

private data class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankPulseAppShell(
    viewModel: RankPulseViewModel,
    currentUserId: String
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val reviewsState by viewModel.reviewsState.collectAsStateWithLifecycle()
    val competitorsState by viewModel.competitorsState.collectAsStateWithLifecycle()
    val keywordsState by viewModel.keywordsState.collectAsStateWithLifecycle()
    val teamState by viewModel.teamMembersState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val analyticsLogsState by viewModel.analyticsLogsState.collectAsStateWithLifecycle()

    val copyOptState by viewModel.copyOptimizerState.collectAsStateWithLifecycle()
    val kwSuggestState by viewModel.keywordSuggestionsState.collectAsStateWithLifecycle()
    val brandKitState by viewModel.brandKitState.collectAsStateWithLifecycle()
    val aiLogoState by viewModel.aiLogoImageState.collectAsStateWithLifecycle()
    val aiPostImgState by viewModel.aiPostImageState.collectAsStateWithLifecycle()
    val aiPostCopyState by viewModel.aiPostCopyState.collectAsStateWithLifecycle()
    val reviewDrafts by viewModel.reviewDrafts.collectAsStateWithLifecycle()
    val aiBusyReviewId by viewModel.aiBusyReviewId.collectAsStateWithLifecycle()
    val aiBusyCompId by viewModel.aiBusyCompetitorId.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatStreaming by viewModel.chatStreaming.collectAsStateWithLifecycle()
    val decryptedVault by viewModel.decryptedVaultText.collectAsStateWithLifecycle()

    var showNotifications by remember { mutableStateOf(false) }
    var showWelcomeOverride by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                is UiEvent.ShareUri -> {
                    ReportExporter.shareFileAndText(
                        context = context,
                        uri = event.uri,
                        mimeType = event.mimeType,
                        text = event.text,
                        targetPackage = event.targetPackage
                    )
                }
            }
        }
    }

    val destinations = remember(strings) {
        listOf(
            NavDestination(ROUTE_DASHBOARD, strings.navDashboard, Icons.Default.Dashboard),
            NavDestination(ROUTE_SOCIAL, strings.navSocial, Icons.Default.PhotoLibrary),
            NavDestination(ROUTE_AI_STUDIO, strings.navAiStudio, Icons.Default.AutoAwesome),
            NavDestination(ROUTE_INSIGHTS, strings.navInsights, Icons.Default.Analytics),
            NavDestination(ROUTE_WORKSPACE, strings.navWorkspace, Icons.Default.Business)
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    val profile = (profileState as? UiState.Success)?.data ?: BusinessProfile(userId = currentUserId)
    val posts = (postsState as? UiState.Success)?.data ?: emptyList()
    val reviews = (reviewsState as? UiState.Success)?.data ?: emptyList()
    val competitors = (competitorsState as? UiState.Success)?.data ?: emptyList()
    val keywords = (keywordsState as? UiState.Success)?.data ?: emptyList()
    val teamMembers = (teamState as? UiState.Success)?.data ?: emptyList()
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()
    val analyticsLogs = (analyticsLogsState as? UiState.Success)?.data ?: emptyList()

    val unreadCount = notifications.count { !it.readFlag }
    val showWelcomePopup = showWelcomeOverride ||
        (profileState is UiState.Success && prefs.onboardingShownForUid != currentUserId)

    LaunchedEffect(showWelcomePopup, profileState) {
        if (!showWelcomePopup && profileState is UiState.Success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                withFrameNanos { }
                delay(350)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val canEdit = prefs.activePreviewRole.canEditContent
    val canReplyOrAnalyze = prefs.activePreviewRole.canReplyOrAnalyze
    val canDelete = prefs.activePreviewRole.canReplyOrAnalyze

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${prefs.activePreviewRole.label} • ${prefs.language.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        // Quick Hindi / English toggle
                        TextButton(
                            onClick = {
                                val next = if (prefs.language == AppLanguage.ENGLISH) {
                                    AppLanguage.HINDI
                                } else {
                                    AppLanguage.ENGLISH
                                }
                                viewModel.setLanguage(next)
                            },
                            modifier = Modifier.testTag("topbar_lang_toggle")
                        ) {
                            Text(
                                text = if (prefs.language == AppLanguage.ENGLISH) "हिन्दी" else "EN",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Quick Theme Color Palette picker
                        IconButton(
                            onClick = { showThemePicker = true },
                            modifier = Modifier.testTag("topbar_palette_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = strings.themeColorLabel,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Quick Dark Mode toggle
                        IconButton(
                            onClick = {
                                val next = if (prefs.theme == ThemePreference.DARK) {
                                    ThemePreference.LIGHT
                                } else {
                                    ThemePreference.DARK
                                }
                                viewModel.setTheme(next)
                            },
                            modifier = Modifier.testTag("topbar_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (prefs.theme == ThemePreference.DARK) {
                                    Icons.Default.LightMode
                                } else {
                                    Icons.Default.DarkMode
                                }
                                ,
                                contentDescription = strings.darkModeLabel
                            )
                        }
                        // Real-time Notifications
                        IconButton(
                            onClick = { showNotifications = true },
                            modifier = Modifier.testTag("topbar_notifications_btn")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge { Text(unreadCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = strings.notificationsTitle
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        destinations.forEach { dest ->
                            NavigationBarItem(
                                selected = currentRoute == dest.route,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        destinations.forEach { dest ->
                            NavigationRailItem(
                                selected = currentRoute == dest.route,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (!isOnline) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${strings.offlineBannerTitle}: ${strings.offlineBannerSubtitle}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    if (!canEdit) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.readOnlyRoleWarning,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    when (val pState = profileState) {
                        is UiState.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is UiState.Error -> {
                            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = pState.message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        is UiState.Success -> {
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_DASHBOARD,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(ROUTE_DASHBOARD) {
                                    DashboardScreen(
                                        profile = profile,
                                        analyticsLogs = analyticsLogs,
                                        reviews = reviews,
                                        keywords = keywords,
                                        posts = posts,
                                        competitors = competitors,
                                        aiBusyCompetitorId = aiBusyCompId,
                                        isOnline = isOnline,
                                        canEdit = canEdit,
                                        canReplyOrAnalyze = canReplyOrAnalyze,
                                        onOpenWelcome = { showWelcomeOverride = true },
                                        onLogMetric = { l, c, v ->
                                            viewModel.recordLeadCallView(profile, l, c, v)
                                        },
                                        onUpdateGmb = { status, url, cta ->
                                            viewModel.updateGmbAndChannels(
                                                gmbStatus = status,
                                                gmbPlaceUrl = url,
                                                gmbCtaType = cta,
                                                whatsappNumber = profile.whatsappNumber,
                                                instagramHandle = profile.instagramHandle,
                                                facebookPage = profile.facebookPage,
                                                autoReplyReviews = profile.autoReplyReviews,
                                                dailyAutoPost = profile.dailyAutoPost,
                                                monthlyWhatsappReport = profile.monthlyWhatsappReport
                                            )
                                        },
                                        onAddCompetitor = { name, web, rank, rating, count, viewsEst, seoScore ->
                                            viewModel.addCompetitor(name, web, rank, rating, count, viewsEst, seoScore)
                                        },
                                        onAnalyzeCompetitor = { comp ->
                                            viewModel.analyzeCompetitorWithAi(profile, comp, reviews, keywords, posts)
                                        },
                                        onDeleteCompetitor = { compId ->
                                            viewModel.deleteCompetitor(compId)
                                        },
                                        onExportPdf = {
                                            viewModel.exportPdfReport(profile, analyticsLogs, keywords, competitors, posts)
                                        },
                                        onExportCsv = {
                                            viewModel.exportCsvReport(profile, analyticsLogs, keywords)
                                        },
                                        onSendWhatsappReport = {
                                            viewModel.sendMonthlyWhatsappReport(profile, analyticsLogs, keywords, competitors, posts)
                                        }
                                    )
                                }

                                composable(ROUTE_SOCIAL) {
                                    SocialPostsScreen(
                                        profile = profile,
                                        posts = posts,
                                        aiPostCopyState = aiPostCopyState,
                                        aiPostImageState = aiPostImgState,
                                        canEdit = canEdit,
                                        canDelete = canDelete,
                                        onSaveChannels = { wa, ig, fb, dailyAuto, monthlyWa ->
                                            viewModel.updateGmbAndChannels(
                                                gmbStatus = profile.gmbStatus,
                                                gmbPlaceUrl = profile.gmbPlaceUrl,
                                                gmbCtaType = profile.gmbCtaType,
                                                whatsappNumber = wa,
                                                instagramHandle = ig,
                                                facebookPage = fb,
                                                autoReplyReviews = profile.autoReplyReviews,
                                                dailyAutoPost = dailyAuto,
                                                monthlyWhatsappReport = monthlyWa
                                            )
                                        },
                                        onUploadOwnerPhoto = { uri, modalPost ->
                                            viewModel.uploadOwnerPhoto(uri, profile, modalPost)
                                        },
                                        onUploadServiceLogo = { uri, modalPost ->
                                            viewModel.uploadServiceLogo(uri, profile, modalPost)
                                        },
                                        onUpdateProfileInModal = { biz, owner, phone, addr ->
                                            viewModel.updateBusinessProfile(
                                                businessName = biz,
                                                category = profile.category,
                                                ownerName = owner,
                                                ownerPhone = phone,
                                                address = addr,
                                                websiteUrl = profile.websiteUrl,
                                                description = profile.description,
                                                ownerPhotoPath = profile.ownerPhotoPath,
                                                serviceLogoPath = profile.serviceLogoPath
                                            )
                                        },
                                        onGenerateAiPostCopy = { topic, workBy ->
                                            viewModel.generateAiPostCopy(profile, topic, workBy)
                                        },
                                        onGenerateAiPostImage = { topic ->
                                            viewModel.generateAiPostSceneImage(profile, topic)
                                        },
                                        onCreatePost = { title, desc, workBy, channels, status, sched, cta, img, downloadOnPub ->
                                            viewModel.createBrandedPost(profile, title, desc, workBy, channels, status, sched, cta, img, downloadOnPub)
                                        },
                                        onUpdatePostInModal = { updated -> viewModel.updatePostInModal(updated) },
                                        onPublishPost = { post, downloadAfter -> viewModel.publishPost(post, profile, downloadAfter) },
                                        onDeletePost = { postId -> viewModel.deletePost(postId) },
                                        onSharePoster = { post, pkg -> viewModel.sharePostPoster(post, profile, pkg) },
                                        onDownloadPoster = { post -> viewModel.downloadPostPoster(post, profile) }
                                    )
                                }

                                composable(ROUTE_AI_STUDIO) {
                                    AiStudioScreen(
                                        profile = profile,
                                        copyOptimizerState = copyOptState,
                                        keywordSuggestionsState = kwSuggestState,
                                        brandKitState = brandKitState,
                                        aiLogoImageState = aiLogoState,
                                        chatMessages = chatMessages,
                                        chatStreaming = chatStreaming,
                                        canEdit = canEdit,
                                        onOptimizeCopy = { topic, existing ->
                                            viewModel.optimizeWebsiteCopy(profile, topic, existing)
                                        },
                                        onGenerateKeywords = { seed ->
                                            viewModel.generateKeywords(profile, seed)
                                        },
                                        onSaveKeyword = { kw -> viewModel.saveKeywordToTracker(kw) },
                                        onGenerateBrandKit = { viewModel.generateBrandIdentity(profile) },
                                        onApplyBrandDescription = { newDesc ->
                                            viewModel.updateBusinessProfile(
                                                businessName = profile.businessName,
                                                category = profile.category,
                                                ownerName = profile.ownerName,
                                                ownerPhone = profile.ownerPhone,
                                                address = profile.address,
                                                websiteUrl = profile.websiteUrl,
                                                description = newDesc,
                                                ownerPhotoPath = profile.ownerPhotoPath,
                                                serviceLogoPath = profile.serviceLogoPath
                                            )
                                        },
                                        onGenerateAiLogo = { viewModel.generateAiServiceLogo(profile) },
                                        onSendChat = { msg -> viewModel.sendChatMessage(profile, msg) }
                                    )
                                }

                                composable(ROUTE_INSIGHTS) {
                                    InsightsScreen(
                                        profile = profile,
                                        reviews = reviews,
                                        competitors = competitors,
                                        keywords = keywords,
                                        reviewDrafts = reviewDrafts,
                                        aiBusyReviewId = aiBusyReviewId,
                                        aiBusyCompetitorId = aiBusyCompId,
                                        canEdit = canEdit,
                                        canReplyOrAnalyze = canReplyOrAnalyze,
                                        onToggleAutoReply = { enabled ->
                                            viewModel.updateGmbAndChannels(
                                                gmbStatus = profile.gmbStatus,
                                                gmbPlaceUrl = profile.gmbPlaceUrl,
                                                gmbCtaType = profile.gmbCtaType,
                                                whatsappNumber = profile.whatsappNumber,
                                                instagramHandle = profile.instagramHandle,
                                                facebookPage = profile.facebookPage,
                                                autoReplyReviews = enabled,
                                                dailyAutoPost = profile.dailyAutoPost,
                                                monthlyWhatsappReport = profile.monthlyWhatsappReport
                                            )
                                        },
                                        onAddReview = { name, rating, comment ->
                                            viewModel.addCustomerReview(profile, name, rating, comment)
                                        },
                                        onGenerateAiReviewReply = { rev ->
                                            viewModel.generateAndSendReviewReply(profile, rev, autoPublish = false)
                                        },
                                        onPublishReviewReply = { revId, text ->
                                            viewModel.publishReviewReply(revId, text)
                                        },
                                        onAddCompetitor = { name, web, rank, rating, count, viewsEst, seoScore ->
                                            viewModel.addCompetitor(name, web, rank, rating, count, viewsEst, seoScore)
                                        },
                                        onAnalyzeCompetitor = { comp ->
                                            viewModel.analyzeCompetitorWithAi(profile, comp, reviews, keywords, posts)
                                        },
                                        onDeleteCompetitor = { compId ->
                                            viewModel.deleteCompetitor(compId)
                                        }
                                    )
                                }

                                composable(ROUTE_WORKSPACE) {
                                    WorkspaceScreen(
                                        profile = profile,
                                        teamMembers = teamMembers,
                                        prefs = prefs,
                                        decryptedVaultNote = decryptedVault,
                                        onSaveProfile = { biz, cat, owner, phone, addr, web, desc ->
                                            viewModel.updateBusinessProfile(
                                                businessName = biz,
                                                category = cat,
                                                ownerName = owner,
                                                ownerPhone = phone,
                                                address = addr,
                                                websiteUrl = web,
                                                description = desc,
                                                ownerPhotoPath = profile.ownerPhotoPath,
                                                serviceLogoPath = profile.serviceLogoPath
                                            )
                                        },
                                        onUploadOwnerPhoto = { uri -> viewModel.uploadOwnerPhoto(uri, profile) },
                                        onUploadServiceLogo = { uri -> viewModel.uploadServiceLogo(uri, profile) },
                                        onUpdateSubscription = { tier, cycle, trial, pay ->
                                            viewModel.updateSubscription(tier, cycle, trial, pay)
                                        },
                                        onSetPreviewRole = { role -> viewModel.setPreviewRole(role) },
                                        onAddTeamMember = { n, e, r -> viewModel.addTeamMember(n, e, r) },
                                        onRemoveTeamMember = { id -> viewModel.removeTeamMember(id) },
                                        onSaveEncryptedVaultNote = { note -> viewModel.saveEncryptedVaultNote(note) },
                                        onUpdatePrivacy = { gdpr, e2ee -> viewModel.updatePrivacyAndGdpr(gdpr, e2ee) },
                                        onExportGdpr = { viewModel.exportGdprData(profile, posts, keywords) },
                                        onEraseGdpr = { viewModel.eraseWorkspaceGdpr() },
                                        onSetTheme = { t -> viewModel.setTheme(t) },
                                        onSetColorPalette = { p -> viewModel.setColorPalette(p) },
                                        onSetLanguage = { l -> viewModel.setLanguage(l) },
                                        onTriggerTestAlert = {
                                            viewModel.triggerCriticalSystemAlert(
                                                "Critical SEO Monitor",
                                                "Google Business Profile visibility check completed. All channels active."
                                            )
                                        },
                                        onSignOut = {
                                            scope.launch {
                                                withContext(NonCancellable) {
                                                    try {
                                                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                                    } catch (e: Exception) {
                                                        Log.w("Auth", "Failed to clear credential state: ${e.message}")
                                                    } finally {
                                                        Firebase.auth.signOut()
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWelcomePopup && profileState is UiState.Success) {
        FirstLoginWelcomeDialog(
            profile = profile,
            onCreateGmb = { biz, phone, addr ->
                viewModel.updateBusinessProfile(
                    businessName = biz.ifBlank { profile.businessName },
                    category = profile.category,
                    ownerName = profile.ownerName,
                    ownerPhone = phone,
                    address = addr,
                    websiteUrl = profile.websiteUrl,
                    description = profile.description,
                    ownerPhotoPath = profile.ownerPhotoPath,
                    serviceLogoPath = profile.serviceLogoPath
                )
                viewModel.updateGmbAndChannels(
                    gmbStatus = "creating",
                    gmbPlaceUrl = profile.gmbPlaceUrl,
                    gmbCtaType = profile.gmbCtaType,
                    whatsappNumber = phone.ifBlank { profile.whatsappNumber },
                    instagramHandle = profile.instagramHandle,
                    facebookPage = profile.facebookPage,
                    autoReplyReviews = profile.autoReplyReviews,
                    dailyAutoPost = profile.dailyAutoPost,
                    monthlyWhatsappReport = profile.monthlyWhatsappReport
                )
                showWelcomeOverride = false
                viewModel.dismissFirstLoginPopup()
            },
            onLinkGmb = { biz, phone, addr, url ->
                viewModel.updateBusinessProfile(
                    businessName = biz.ifBlank { profile.businessName },
                    category = profile.category,
                    ownerName = profile.ownerName,
                    ownerPhone = phone,
                    address = addr,
                    websiteUrl = profile.websiteUrl,
                    description = profile.description,
                    ownerPhotoPath = profile.ownerPhotoPath,
                    serviceLogoPath = profile.serviceLogoPath
                )
                viewModel.updateGmbAndChannels(
                    gmbStatus = "linked",
                    gmbPlaceUrl = url,
                    gmbCtaType = profile.gmbCtaType,
                    whatsappNumber = phone.ifBlank { profile.whatsappNumber },
                    instagramHandle = profile.instagramHandle,
                    facebookPage = profile.facebookPage,
                    autoReplyReviews = profile.autoReplyReviews,
                    dailyAutoPost = profile.dailyAutoPost,
                    monthlyWhatsappReport = profile.monthlyWhatsappReport
                )
                showWelcomeOverride = false
                viewModel.dismissFirstLoginPopup()
            },
            onDismiss = {
                showWelcomeOverride = false
                viewModel.dismissFirstLoginPopup()
            }
        )
    }

    if (showNotifications) {
        NotificationsSheet(
            notifications = notifications,
            onMarkAllRead = { viewModel.markAllNotificationsRead(notifications) },
            onDismiss = { showNotifications = false }
        )
    }

    if (showThemePicker) {
        ThemeColorPickerDialog(
            currentTheme = prefs.theme,
            currentPalette = prefs.colorPalette,
            language = prefs.language,
            onSelectTheme = { viewModel.setTheme(it) },
            onSelectPalette = { viewModel.setColorPalette(it) },
            onDismiss = { showThemePicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeColorPickerDialog(
    currentTheme: ThemePreference,
    currentPalette: ThemeColorPalette,
    language: AppLanguage,
    onSelectTheme: (ThemePreference) -> Unit,
    onSelectPalette: (ThemeColorPalette) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.themeColorLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.darkModeLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePreference.entries.forEach { theme ->
                        FilterChip(
                            selected = currentTheme == theme,
                            onClick = { onSelectTheme(theme) },
                            label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Text(strings.themeColorLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ThemeColorPalette.entries.forEach { palette ->
                        val label = if (language == AppLanguage.HINDI) palette.labelHi else palette.label
                        FilterChip(
                            selected = currentPalette == palette,
                            onClick = { onSelectPalette(palette) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(palette.previewHex))
                                )
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag("dialog_palette_${palette.name}")
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(48.dp)
            ) {
                Text("Done")
            }
        }
    )
}
