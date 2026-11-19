package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.i18n.LocalStrings
import com.example.model.BusinessProfile
import com.example.model.CompetitorEntry
import com.example.model.CustomerReview
import com.example.model.KeywordTarget

@Composable
fun InsightsScreen(
    profile: BusinessProfile,
    reviews: List<CustomerReview>,
    competitors: List<CompetitorEntry>,
    keywords: List<KeywordTarget>,
    reviewDrafts: Map<String, String>,
    aiBusyReviewId: String?,
    aiBusyCompetitorId: String?,
    canEdit: Boolean,
    canReplyOrAnalyze: Boolean,
    onToggleAutoReply: (Boolean) -> Unit,
    onAddReview: (name: String, rating: Long, comment: String) -> Unit,
    onGenerateAiReviewReply: (CustomerReview) -> Unit,
    onPublishReviewReply: (reviewId: String, replyText: String) -> Unit,
    onAddCompetitor: (
        name: String,
        website: String,
        rank: Long,
        rating: Double,
        reviewsCount: Long,
        monthlyViewsEst: Long,
        seoScore: Long
    ) -> Unit,
    onAnalyzeCompetitor: (CompetitorEntry) -> Unit,
    onDeleteCompetitor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    var reviewerName by remember { mutableStateOf("") }
    var reviewRatingText by remember { mutableStateOf("5") }
    var reviewComment by remember { mutableStateOf("") }

    var compName by remember { mutableStateOf("") }
    var compWebsite by remember { mutableStateOf("") }
    var compRankText by remember { mutableStateOf("2") }
    var compRatingText by remember { mutableStateOf("4.4") }
    var compReviewsText by remember { mutableStateOf("80") }
    var compViewsText by remember { mutableStateOf("1400") }
    var compSeoText by remember { mutableStateOf("72") }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Reviews & Automatic AI Reply
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
                            text = strings.reviewsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.autoReplyToggle, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = profile.autoReplyReviews,
                                onCheckedChange = { if (canReplyOrAnalyze) onToggleAutoReply(it) },
                                modifier = Modifier.testTag("auto_reply_switch")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = reviewerName,
                                onValueChange = { reviewerName = it },
                                label = { Text("Customer Name") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = reviewRatingText,
                                onValueChange = { reviewRatingText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Stars (1-5)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = reviewComment,
                            onValueChange = { reviewComment = it },
                            label = { Text("Customer Review Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (reviewerName.isNotBlank() && reviewComment.isNotBlank()) {
                                    val stars = (reviewRatingText.toLongOrNull() ?: 5L).coerceIn(1L, 5L)
                                    onAddReview(reviewerName, stars, reviewComment)
                                    reviewerName = ""
                                    reviewComment = ""
                                }
                            },
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_review_btn")
                        ) {
                            Text(if (profile.autoReplyReviews) "Add Review (Auto-Replies with AI)" else "Log Customer Review")
                        }
                    }
                }
            }

            items(reviews, key = { it.id }) { rev ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    var replyInput by remember(rev.id, reviewDrafts[rev.id], rev.replyText) {
                        mutableStateOf(reviewDrafts[rev.id] ?: rev.replyText)
                    }
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rev.reviewerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("${rev.rating}/5", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Text(rev.comment, style = MaterialTheme.typography.bodyMedium)
                        if (rev.replied) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${if (rev.autoReplied) "Auto-Replied" else "Owner Reply"}: ${rev.replyText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = replyInput,
                                onValueChange = { replyInput = it },
                                label = { Text("Owner Reply") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onGenerateAiReviewReply(rev) },
                                    enabled = aiBusyReviewId != rev.id && canReplyOrAnalyze,
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    if (aiBusyReviewId == rev.id) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.generateAiReply)
                                }
                                Button(
                                    onClick = { if (replyInput.isNotBlank()) onPublishReviewReply(rev.id, replyInput) },
                                    enabled = canReplyOrAnalyze && replyInput.isNotBlank(),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Text("Publish Reply")
                                }
                            }
                        }
                    }
                }
            }

            // 2. Competitor Analysis
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.competitorsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(
                            value = compName,
                            onValueChange = { compName = it },
                            label = { Text("Competitor Business Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = compWebsite,
                            onValueChange = { compWebsite = it },
                            label = { Text("Competitor Website") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = compRankText,
                                onValueChange = { compRankText = it.filter { c -> c.isDigit() } },
                                label = { Text("Map Rank") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = compRatingText,
                                onValueChange = { compRatingText = it },
                                label = { Text("Rating") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = compReviewsText,
                                onValueChange = { compReviewsText = it.filter { c -> c.isDigit() } },
                                label = { Text("Reviews") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = compViewsText,
                                onValueChange = { compViewsText = it.filter { c -> c.isDigit() } },
                                label = { Text("Views / mo") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = compSeoText,
                                onValueChange = { compSeoText = it.filter { c -> c.isDigit() } },
                                label = { Text("SEO Score (0-100)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = {
                                if (compName.isNotBlank()) {
                                    onAddCompetitor(
                                        compName,
                                        compWebsite,
                                        (compRankText.toLongOrNull() ?: 3L).coerceIn(1L, 100L),
                                        (compRatingText.toDoubleOrNull() ?: 4.2).coerceIn(0.0, 5.0),
                                        compReviewsText.toLongOrNull() ?: 0L,
                                        compViewsText.toLongOrNull() ?: 0L,
                                        (compSeoText.toLongOrNull() ?: 50L).coerceIn(0L, 100L)
                                    )
                                    compName = ""
                                    compWebsite = ""
                                }
                            },
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_competitor_btn")
                        ) {
                            Text("Track Competitor")
                        }
                    }
                }
            }

            items(competitors, key = { it.id }) { comp ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(comp.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Rank #${comp.localRank} • ${comp.rating}★ (${comp.reviewCount} rev) • ${comp.monthlyViewsEst} views/mo • SEO ${comp.seoScore}/100",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { onAnalyzeCompetitor(comp) },
                                    enabled = aiBusyCompetitorId != comp.id && canReplyOrAnalyze
                                ) {
                                    if (aiBusyCompetitorId == comp.id) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.analyzeCompetitorAi)
                                }
                                if (canReplyOrAnalyze) {
                                    OutlinedButton(
                                        onClick = { onDeleteCompetitor(comp.id) },
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                        if (comp.aiGapSummary.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = comp.aiGapSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Tracked Keywords
            item {
                Text(
                    text = "${strings.keywordsTitle} (${keywords.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(keywords, key = { it.id }) { kw ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("${kw.keyword} • Rank #${kw.currentRank}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${kw.intent} • ${kw.difficulty}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        if (kw.aiTip.isNotBlank()) {
                            Text(kw.aiTip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
