package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.i18n.LocalStrings
import com.example.model.BusinessProfile
import com.example.model.SeoPost
import com.example.ui.components.BrandedPosterPreview
import com.example.ui.viewmodel.AiResultState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SocialPostsScreen(
    profile: BusinessProfile,
    posts: List<SeoPost>,
    aiPostCopyState: AiResultState<Pair<String, String>>,
    aiPostImageState: AiResultState<String>,
    canEdit: Boolean,
    canDelete: Boolean,
    onSaveChannels: (whatsapp: String, ig: String, fb: String, dailyAuto: Boolean, monthlyWa: Boolean) -> Unit,
    onUploadOwnerPhoto: (Uri, SeoPost?) -> Unit,
    onUploadServiceLogo: (Uri, SeoPost?) -> Unit,
    onUpdateProfileInModal: (bizName: String, ownerName: String, phone: String, address: String) -> Unit,
    onGenerateAiPostCopy: (topic: String, workBy: String) -> Unit,
    onGenerateAiPostImage: (topic: String) -> Unit,
    onCreatePost: (
        title: String,
        desc: String,
        workBy: String,
        channels: List<String>,
        status: String,
        schedule: String,
        cta: String,
        posterImg: String,
        downloadOnPublish: Boolean
    ) -> Unit,
    onUpdatePostInModal: (SeoPost) -> Unit,
    onPublishPost: (SeoPost, Boolean) -> Unit,
    onDeletePost: (String) -> Unit,
    onSharePoster: (SeoPost, String?) -> Unit,
    onDownloadPoster: (SeoPost) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    var waInput by remember(profile.whatsappNumber) { mutableStateOf(profile.whatsappNumber) }
    var igInput by remember(profile.instagramHandle) { mutableStateOf(profile.instagramHandle) }
    var fbInput by remember(profile.facebookPage) { mutableStateOf(profile.facebookPage) }
    var dailyAuto by remember(profile.dailyAutoPost) { mutableStateOf(profile.dailyAutoPost) }
    var monthlyWa by remember(profile.monthlyWhatsappReport) { mutableStateOf(profile.monthlyWhatsappReport) }

    var postTitle by remember { mutableStateOf("") }
    var postDesc by remember { mutableStateOf("") }
    var workBy by remember { mutableStateOf("") }
    var scheduleTime by remember { mutableStateOf("Daily 09:00 AM") }
    var posterScenePath by remember { mutableStateOf("") }
    var autoDownloadOnPublish by remember { mutableStateOf(true) }
    var selectedChannels by remember {
        mutableStateOf(listOf("WHATSAPP", "INSTAGRAM", "FACEBOOK", "GMB", "WEBSITE"))
    }

    var activeModalPost by remember { mutableStateOf<SeoPost?>(null) }

    LaunchedEffect(aiPostCopyState) {
        if (aiPostCopyState is AiResultState.Success) {
            postTitle = aiPostCopyState.data.first
            postDesc = aiPostCopyState.data.second
        }
    }
    LaunchedEffect(aiPostImageState) {
        if (aiPostImageState is AiResultState.Success) {
            posterScenePath = aiPostImageState.data
        }
    }

    val ownerPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUploadOwnerPhoto(uri, activeModalPost)
        }
    }
    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUploadServiceLogo(uri, activeModalPost)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Connected Channels & Daily Auto-Schedule
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.socialChannelsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = waInput,
                            onValueChange = { waInput = it },
                            label = { Text("WhatsApp Business Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("channel_whatsapp_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = igInput,
                                onValueChange = { igInput = it },
                                label = { Text("Instagram Page") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("channel_ig_input")
                            )
                            OutlinedTextField(
                                value = fbInput,
                                onValueChange = { fbInput = it },
                                label = { Text("Facebook Page") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("channel_fb_input")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.scheduleDailyLabel, style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = dailyAuto, onCheckedChange = { dailyAuto = it })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Monthly WhatsApp Report Auto-Prep", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = monthlyWa, onCheckedChange = { monthlyWa = it })
                        }
                        Button(
                            onClick = { onSaveChannels(waInput, igInput, fbInput, dailyAuto, monthlyWa) },
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_channels_btn")
                        ) {
                            Text("Save Connected Pages & Schedule")
                        }
                    }
                }
            }

            // 2. Post Composer + Branded Owner Poster Live Builder
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = strings.createPostTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    ownerPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = canEdit,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("upload_owner_photo_btn")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.changeOwnerPhoto, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    logoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = canEdit,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("upload_logo_btn")
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.changeServiceLogo, maxLines = 1)
                            }
                        }

                        OutlinedTextField(
                            value = postTitle,
                            onValueChange = { postTitle = it },
                            label = { Text(strings.postTitleHint) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("post_title_input")
                        )
                        OutlinedTextField(
                            value = postDesc,
                            onValueChange = { postDesc = it },
                            label = { Text(strings.postDescHint) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth().testTag("post_desc_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = workBy,
                                onValueChange = { workBy = it },
                                label = { Text(strings.workDoneByHint) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("post_work_by_input")
                            )
                            OutlinedTextField(
                                value = scheduleTime,
                                onValueChange = { scheduleTime = it },
                                label = { Text("Schedule Slot") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("post_schedule_input")
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("WHATSAPP", "INSTAGRAM", "FACEBOOK", "GMB", "WEBSITE").forEach { ch ->
                                val selected = ch in selectedChannels
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedChannels = if (selected && selectedChannels.size > 1) {
                                            selectedChannels - ch
                                        } else if (!selected) {
                                            selectedChannels + ch
                                        } else selectedChannels
                                    },
                                    label = { Text(ch) }
                                )
                            }
                        }

                        // AI Post Copy & AI Poster Scene Image
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onGenerateAiPostCopy(
                                        postTitle.ifBlank { profile.category },
                                        workBy.ifBlank { profile.ownerName }
                                    )
                                },
                                enabled = aiPostCopyState !is AiResultState.Loading,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("ai_post_copy_btn")
                            ) {
                                if (aiPostCopyState is AiResultState.Loading) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text("AI Post Copy")
                            }
                            OutlinedButton(
                                onClick = { onGenerateAiPostImage(postTitle.ifBlank { profile.category }) },
                                enabled = aiPostImageState !is AiResultState.Loading,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("ai_post_art_btn")
                            ) {
                                if (aiPostImageState is AiResultState.Loading) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(strings.generatePosterScene)
                            }
                        }

                        if (aiPostCopyState is AiResultState.Error) {
                            Text(aiPostCopyState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        if (aiPostImageState is AiResultState.Error) {
                            Text(aiPostImageState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        // Live Branded Poster Preview
                        Text(
                            text = "${strings.brandedPosterPreview} (Auto-embeds Owner Photo, Logo, Phone & Address)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        BrandedPosterPreview(
                            post = SeoPost(
                                title = postTitle.ifBlank { "${profile.businessName} Daily Update" },
                                description = postDesc.ifBlank { profile.description.ifBlank { "Trusted local experts ready to serve." } },
                                workPerformedBy = workBy.ifBlank { profile.ownerName },
                                channels = selectedChannels,
                                status = "SCHEDULED",
                                ctaLabel = profile.gmbCtaType,
                                posterImagePath = posterScenePath
                            ),
                            profile = profile
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Download poster image when publishing",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = autoDownloadOnPublish,
                                onCheckedChange = { autoDownloadOnPublish = it },
                                modifier = Modifier.testTag("auto_download_publish_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onCreatePost(
                                        postTitle.ifBlank { "${profile.businessName} Daily Offer" },
                                        postDesc.ifBlank { profile.description.ifBlank { "Quality local service today!" } },
                                        workBy.ifBlank { profile.ownerName },
                                        selectedChannels,
                                        "SCHEDULED",
                                        scheduleTime,
                                        profile.gmbCtaType,
                                        posterScenePath,
                                        false
                                    )
                                    postTitle = ""
                                    postDesc = ""
                                },
                                enabled = canEdit,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("schedule_post_btn")
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.schedulePost)
                            }
                            Button(
                                onClick = {
                                    onCreatePost(
                                        postTitle.ifBlank { "${profile.businessName} Update" },
                                        postDesc.ifBlank { profile.description.ifBlank { "Quality local service today!" } },
                                        workBy.ifBlank { profile.ownerName },
                                        selectedChannels,
                                        "PUBLISHED",
                                        scheduleTime,
                                        profile.gmbCtaType,
                                        posterScenePath,
                                        autoDownloadOnPublish
                                    )
                                    postTitle = ""
                                    postDesc = ""
                                },
                                enabled = canEdit,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("publish_post_btn")
                            ) {
                                Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (autoDownloadOnPublish) strings.publishAndDownload else strings.publishPost, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 3. Scheduled & Published Posts Feed (Tap any post to open Modal View)
            item {
                Text(
                    text = "Scheduled & Published Posters (${posts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (posts.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "No posters created yet. Use the Branded Poster builder above to schedule daily updates across WhatsApp, Instagram, Facebook, GMB & Website.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeModalPost = post }
                            .testTag("post_card_${post.id}"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BrandedPosterPreview(post = post, profile = profile)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { activeModalPost = post }) {
                                Text("Edit Modal")
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onDownloadPoster(post) },
                                    modifier = Modifier.height(40.dp).testTag("download_poster_${post.id}")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = strings.downloadPoster, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Download")
                                }
                                OutlinedButton(
                                    onClick = { onSharePoster(post, "com.whatsapp") },
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text("WhatsApp")
                                }
                                IconButton(onClick = { onSharePoster(post, null) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Poster")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal View for EVERY Post with Owner Photo Upload, Profile Management, Download & Share
    val modalPost = activeModalPost
    if (modalPost != null) {
        PostDetailModalDialog(
            post = modalPost,
            profile = profile,
            canEdit = canEdit,
            canDelete = canDelete,
            onUploadOwnerPhoto = {
                ownerPhotoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onUploadServiceLogo = {
                logoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSaveProfileAndPost = { bizName, ownerName, phone, address, title, desc, staff ->
                onUpdateProfileInModal(bizName, ownerName, phone, address)
                onUpdatePostInModal(
                    modalPost.copy(
                        title = title,
                        description = desc,
                        workPerformedBy = staff,
                        ownerPhotoSnapshotPath = profile.ownerPhotoPath,
                        serviceLogoSnapshotPath = profile.serviceLogoPath,
                        ownerPhoneSnapshot = phone,
                        addressSnapshot = address
                    )
                )
                activeModalPost = null
            },
            onPublish = {
                onPublishPost(modalPost, true)
                activeModalPost = null
            },
            onDelete = {
                onDeletePost(modalPost.id)
                activeModalPost = null
            },
            onShare = { pkg -> onSharePoster(modalPost, pkg) },
            onDownload = { onDownloadPoster(modalPost) },
            onDismiss = { activeModalPost = null }
        )
    }
}

@Composable
private fun PostDetailModalDialog(
    post: SeoPost,
    profile: BusinessProfile,
    canEdit: Boolean,
    canDelete: Boolean,
    onUploadOwnerPhoto: () -> Unit,
    onUploadServiceLogo: () -> Unit,
    onSaveProfileAndPost: (
        bizName: String,
        ownerName: String,
        phone: String,
        address: String,
        title: String,
        desc: String,
        staff: String
    ) -> Unit,
    onPublish: () -> Unit,
    onDelete: () -> Unit,
    onShare: (String?) -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var bizName by remember(profile.businessName) { mutableStateOf(profile.businessName) }
    var ownerName by remember(profile.ownerName) { mutableStateOf(profile.ownerName) }
    var phone by remember(profile.ownerPhone) { mutableStateOf(profile.ownerPhone) }
    var address by remember(profile.address) { mutableStateOf(profile.address) }
    var title by remember(post.title) { mutableStateOf(post.title) }
    var desc by remember(post.description) { mutableStateOf(post.description) }
    var staff by remember(post.workPerformedBy) { mutableStateOf(post.workPerformedBy) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(14.dp)
                .widthIn(max = 620.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.editProfileInModal,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Modal")
                    }
                }

                BrandedPosterPreview(
                    post = post.copy(
                        title = title,
                        description = desc,
                        workPerformedBy = staff,
                        ownerPhoneSnapshot = phone,
                        addressSnapshot = address
                    ),
                    profile = profile.copy(
                        businessName = bizName,
                        ownerName = ownerName,
                        ownerPhone = phone,
                        address = address
                    )
                )

                // Owner Photo & Logo Upload directly in Modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onUploadOwnerPhoto,
                        enabled = canEdit,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("modal_owner_photo_btn")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.changeOwnerPhoto, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onUploadServiceLogo,
                        enabled = canEdit,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("modal_logo_btn")
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.changeServiceLogo, maxLines = 1)
                    }
                }

                // Editable Owner Profile & Post Fields inside Modal
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Post Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Post Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = staff, onValueChange = { staff = it }, label = { Text("Work Performed By (Staff/Team)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = bizName, onValueChange = { bizName = it }, label = { Text("Business Name") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Owner Phone") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.weight(1f))
                }

                // Share & Save Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onShare("com.whatsapp") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White)
                    ) {
                        Text(strings.shareToWhatsapp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onShare(null) },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.sharePoster, maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("modal_download_poster_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.downloadPoster)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canDelete) {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (post.status != "PUBLISHED" && canEdit) {
                            OutlinedButton(onClick = onPublish) {
                                Text(strings.publishAndDownload)
                            }
                        }
                        Button(
                            onClick = { onSaveProfileAndPost(bizName, ownerName, phone, address, title, desc, staff) },
                            enabled = canEdit,
                            modifier = Modifier.testTag("modal_save_post_btn")
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}
