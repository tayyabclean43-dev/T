package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.AiBrandIdentityResult
import com.example.data.AiKeywordSuggestion
import com.example.i18n.LocalStrings
import com.example.model.BusinessProfile
import com.example.ui.viewmodel.AiResultState
import com.example.ui.viewmodel.ChatMessage
import java.io.File

@Composable
fun AiStudioScreen(
    profile: BusinessProfile,
    copyOptimizerState: AiResultState<String>,
    keywordSuggestionsState: AiResultState<List<AiKeywordSuggestion>>,
    brandKitState: AiResultState<AiBrandIdentityResult>,
    aiLogoImageState: AiResultState<String>,
    chatMessages: List<ChatMessage>,
    chatStreaming: Boolean,
    canEdit: Boolean,
    onOptimizeCopy: (topic: String, existingCopy: String) -> Unit,
    onGenerateKeywords: (seed: String) -> Unit,
    onSaveKeyword: (AiKeywordSuggestion) -> Unit,
    onGenerateBrandKit: () -> Unit,
    onApplyBrandDescription: (desc: String) -> Unit,
    onGenerateAiLogo: () -> Unit,
    onSendChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var selectedTab by remember { mutableIntStateOf(0) }

    var pageTopic by remember { mutableStateOf("Home & Local Services Page") }
    var existingCopy by remember { mutableStateOf(profile.description) }
    var keywordSeed by remember { mutableStateOf(profile.category) }
    var chatInput by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("Chatbot", "SEO Copy", "Keywords", "Logo & CTA")
                    tabs.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            label = { Text(label) },
                            modifier = Modifier.testTag("ai_tab_$idx")
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.aiChatbotTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                if (chatMessages.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = "Ask me how to rank #1 on Google Maps, optimize your Google Business Profile CTA, or schedule Hindi/English posts.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        chatMessages.forEach { msg ->
                                            val bg = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = bg,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = if (msg.isUser) "You" else "RankPulse AI",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(text = msg.text, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = chatInput,
                                        onValueChange = { chatInput = it },
                                        label = { Text(strings.askAiPlaceholder) },
                                        modifier = Modifier.weight(1f).testTag("ai_chat_input")
                                    )
                                    IconButton(
                                        onClick = {
                                            if (chatInput.isNotBlank()) {
                                                onSendChat(chatInput)
                                                chatInput = ""
                                            }
                                        },
                                        enabled = !chatStreaming && chatInput.isNotBlank(),
                                        modifier = Modifier.size(48.dp).testTag("ai_chat_send_btn")
                                    ) {
                                        if (chatStreaming) {
                                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = strings.send, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Web, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.aiCopyOptimizerTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                OutlinedTextField(
                                    value = pageTopic,
                                    onValueChange = { pageTopic = it },
                                    label = { Text("Target Website Page / Service") },
                                    modifier = Modifier.fillMaxWidth().testTag("copy_topic_input")
                                )
                                OutlinedTextField(
                                    value = existingCopy,
                                    onValueChange = { existingCopy = it },
                                    label = { Text("Existing Website Copy (optional)") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth().testTag("copy_existing_input")
                                )
                                Button(
                                    onClick = { onOptimizeCopy(pageTopic, existingCopy) },
                                    enabled = copyOptimizerState !is AiResultState.Loading,
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("optimize_copy_btn")
                                ) {
                                    if (copyOptimizerState is AiResultState.Loading) {
                                        CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(strings.generateWithAi)
                                }

                                when (copyOptimizerState) {
                                    is AiResultState.Success -> {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = copyOptimizerState.data,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(14.dp)
                                            )
                                        }
                                    }
                                    is AiResultState.Error -> {
                                        Text(copyOptimizerState.message, color = MaterialTheme.colorScheme.error)
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.aiKeywordFinderTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                OutlinedTextField(
                                    value = keywordSeed,
                                    onValueChange = { keywordSeed = it },
                                    label = { Text("Service / City Keyword Seed") },
                                    modifier = Modifier.fillMaxWidth().testTag("keyword_seed_input")
                                )
                                Button(
                                    onClick = { onGenerateKeywords(keywordSeed) },
                                    enabled = keywordSuggestionsState !is AiResultState.Loading,
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("generate_keywords_btn")
                                ) {
                                    if (keywordSuggestionsState is AiResultState.Loading) {
                                        CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(strings.generateWithAi)
                                }

                                when (keywordSuggestionsState) {
                                    is AiResultState.Success -> {
                                        keywordSuggestionsState.data.forEach { kw ->
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = kw.keyword,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        TextButton(
                                                            onClick = { onSaveKeyword(kw) },
                                                            enabled = canEdit
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Text("Track")
                                                        }
                                                    }
                                                    Text(
                                                        text = "${kw.intent} • Difficulty: ${kw.difficulty}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(text = kw.aiTip, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                    is AiResultState.Error -> {
                                        Text(keywordSuggestionsState.message, color = MaterialTheme.colorScheme.error)
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.aiLogoDescTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onGenerateBrandKit,
                                        enabled = brandKitState !is AiResultState.Loading,
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("ai_brand_copy_btn")
                                    ) {
                                        Text("AI Description & CTA")
                                    }
                                    OutlinedButton(
                                        onClick = onGenerateAiLogo,
                                        enabled = aiLogoImageState !is AiResultState.Loading && canEdit,
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("ai_generate_logo_btn")
                                    ) {
                                        if (aiLogoImageState is AiResultState.Loading) {
                                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(Modifier.width(6.dp))
                                        } else {
                                            Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text("AI Logo Image")
                                    }
                                }

                                if (profile.serviceLogoPath.isNotBlank() && File(profile.serviceLogoPath).exists()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(File(profile.serviceLogoPath)),
                                        contentDescription = "Current Service Logo",
                                        modifier = Modifier
                                            .size(110.dp)
                                            .clip(RoundedCornerShape(18.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                if (aiLogoImageState is AiResultState.Error) {
                                    Text(aiLogoImageState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }

                                when (brandKitState) {
                                    is AiResultState.Success -> {
                                        val kit = brandKitState.data
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text("Tagline: ${kit.tagline}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text("Recommended GMB CTA: ${kit.ctaLabel}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                                Text("SEO Description: ${kit.seoDescription}", style = MaterialTheme.typography.bodyMedium)
                                                Text("Logo Brief: ${kit.logoConcept}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Button(
                                                    onClick = { onApplyBrandDescription(kit.seoDescription) },
                                                    enabled = canEdit
                                                ) {
                                                    Text("Apply Description to Business Profile")
                                                }
                                            }
                                        }
                                    }
                                    is AiResultState.Error -> {
                                        Text(brandKitState.message, color = MaterialTheme.colorScheme.error)
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
