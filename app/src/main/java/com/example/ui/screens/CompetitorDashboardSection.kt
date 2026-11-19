package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.i18n.LocalStrings
import com.example.model.BusinessProfile
import com.example.model.CompetitorEntry
import com.example.model.CustomerReview
import com.example.model.KeywordTarget
import com.example.model.SeoPost
import java.util.Locale
import kotlin.math.roundToInt

data class UserBenchmarkMetrics(
    val localRank: Long,
    val avgRating: Double,
    val reviewCount: Long,
    val monthlyViews: Long,
    val seoScore: Long,
    val rankFromKeywords: Boolean
)

enum class BenchmarkChartMetric {
    SEO_SCORE,
    MONTHLY_VIEWS,
    REVIEWS,
    MAP_RANK
}

fun calculateUserBenchmark(
    profile: BusinessProfile,
    reviews: List<CustomerReview>,
    keywords: List<KeywordTarget>,
    posts: List<SeoPost>
): UserBenchmarkMetrics {
    val bestKeywordRank = keywords.filter { it.currentRank > 0L }.minOfOrNull { it.currentRank }
    val rankFromKeywords = bestKeywordRank != null
    val localRank = bestKeywordRank ?: if (profile.gmbStatus == "linked") 4L else 15L
    val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0
    val reviewCount = reviews.size.toLong()

    var score = 15L
    when (profile.gmbStatus) {
        "linked" -> score += 25L
        "creating" -> score += 12L
    }
    if (profile.websiteUrl.isNotBlank()) score += 6L
    if (profile.description.isNotBlank()) score += 6L
    if (profile.ownerPhotoPath.isNotBlank() && profile.serviceLogoPath.isNotBlank()) score += 8L
    score += (reviews.size.coerceAtMost(10) * 1L) + (avgRating * 2.0).toLong().coerceAtMost(10L)
    val top10Count = keywords.count { it.currentRank in 1L..10L }
    score += (keywords.size.coerceAtMost(5) * 1L) + (top10Count.coerceAtMost(5) * 2L)
    val publishedPosts = posts.count { it.status == "PUBLISHED" }
    score += (publishedPosts.coerceAtMost(5) * 2L)
    if (profile.monthlyViews >= 500L) score += 5L else if (profile.monthlyViews > 0L) score += 2L

    return UserBenchmarkMetrics(
        localRank = localRank,
        avgRating = avgRating,
        reviewCount = reviewCount,
        monthlyViews = profile.monthlyViews,
        seoScore = score.coerceIn(0L, 100L),
        rankFromKeywords = rankFromKeywords
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompetitorBenchmarkSection(
    profile: BusinessProfile,
    reviews: List<CustomerReview>,
    keywords: List<KeywordTarget>,
    posts: List<SeoPost>,
    competitors: List<CompetitorEntry>,
    aiBusyCompetitorId: String?,
    canEdit: Boolean,
    canReplyOrAnalyze: Boolean,
    onAddCompetitor: (
        name: String,
        website: String,
        rank: Long,
        rating: Double,
        reviews: Long,
        monthlyViewsEst: Long,
        seoScore: Long
    ) -> Unit,
    onAnalyzeCompetitor: (CompetitorEntry) -> Unit,
    onDeleteCompetitor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMetric by remember { mutableStateOf(BenchmarkChartMetric.SEO_SCORE) }

    val userMetrics = remember(profile, reviews, keywords, posts) {
        calculateUserBenchmark(profile, reviews, keywords, posts)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header + Add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = strings.competitorBenchmarkTitle,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = strings.competitorBenchmarkTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.competitorBenchmarkSubtitle} (${competitors.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    enabled = canEdit,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("dashboard_add_competitor_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.trackCompetitorBtn, maxLines = 1)
                }
            }

            if (competitors.isEmpty()) {
                // Live user baseline + empty competitor prompt
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.yourBusinessLabel}: ${profile.businessName}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${strings.seoScoreLabel}: ${userMetrics.seoScore}/100",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = strings.noCompetitorsEmptyTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = strings.noCompetitorsEmptyDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            enabled = canEdit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("dashboard_empty_add_competitor_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.trackCompetitorBtn)
                        }
                    }
                }
            } else {
                // 1. Head-to-Head Scorecard (Your Business vs Competitor Avg)
                HeadToHeadScorecard(
                    profile = profile,
                    userMetrics = userMetrics,
                    competitors = competitors,
                    strings = strings
                )

                // 2. Interactive Visual Benchmark Chart
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BenchmarkChartMetric.entries.forEach { metric ->
                            val label = when (metric) {
                                BenchmarkChartMetric.SEO_SCORE -> strings.seoScoreLabel
                                BenchmarkChartMetric.MONTHLY_VIEWS -> strings.monthlyViewsEstLabel
                                BenchmarkChartMetric.REVIEWS -> strings.reviewsCountLabel
                                BenchmarkChartMetric.MAP_RANK -> strings.localRankLabel
                            }
                            FilterChip(
                                selected = selectedMetric == metric,
                                onClick = { selectedMetric = metric },
                                label = { Text(label) },
                                modifier = Modifier.testTag("benchmark_metric_${metric.name.lowercase()}")
                            )
                        }
                    }

                    CompetitorComparisonBarChart(
                        businessName = profile.businessName,
                        userMetrics = userMetrics,
                        competitors = competitors,
                        selectedMetric = selectedMetric,
                        strings = strings
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                // 3. Individual Competitor Comparison Cards
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    competitors.forEach { comp ->
                        CompetitorComparisonItemCard(
                            competitor = comp,
                            userMetrics = userMetrics,
                            strings = strings,
                            isAnalyzing = aiBusyCompetitorId == comp.id,
                            canReplyOrAnalyze = canReplyOrAnalyze,
                            onAnalyze = { onAnalyzeCompetitor(comp) },
                            onDelete = { onDeleteCompetitor(comp.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCompetitorDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, website, rank, rating, reviewCount, monthlyViewsEst, seoScore ->
                onAddCompetitor(name, website, rank, rating, reviewCount, monthlyViewsEst, seoScore)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun HeadToHeadScorecard(
    profile: BusinessProfile,
    userMetrics: UserBenchmarkMetrics,
    competitors: List<CompetitorEntry>,
    strings: AppStrings
) {
    val avgCompRank = competitors.map { it.localRank }.average().roundToInt().toLong()
    val avgCompRating = competitors.map { it.rating }.average()
    val avgCompReviews = competitors.map { it.reviewCount }.average().roundToInt().toLong()
    val avgCompViews = competitors.map { it.monthlyViewsEst }.average().roundToInt().toLong()
    val avgCompSeo = competitors.map { it.seoScore }.average().roundToInt().toLong()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Metric",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = profile.businessName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = strings.competitorAvgLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.9f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            ScorecardRow(
                label = strings.localRankLabel,
                userValue = "#${userMetrics.localRank}",
                compValue = "#$avgCompRank",
                userIsBetter = userMetrics.localRank < avgCompRank,
                isTied = userMetrics.localRank == avgCompRank,
                strings = strings
            )
            ScorecardRow(
                label = strings.ratingLabel,
                userValue = if (userMetrics.avgRating > 0.0) String.format(Locale.US, "%.1f★", userMetrics.avgRating) else "0.0★",
                compValue = String.format(Locale.US, "%.1f★", avgCompRating),
                userIsBetter = userMetrics.avgRating > avgCompRating,
                isTied = kotlin.math.abs(userMetrics.avgRating - avgCompRating) < 0.05,
                strings = strings
            )
            ScorecardRow(
                label = strings.reviewsCountLabel,
                userValue = userMetrics.reviewCount.toString(),
                compValue = avgCompReviews.toString(),
                userIsBetter = userMetrics.reviewCount > avgCompReviews,
                isTied = userMetrics.reviewCount == avgCompReviews,
                strings = strings
            )
            ScorecardRow(
                label = strings.monthlyViewsEstLabel,
                userValue = userMetrics.monthlyViews.toString(),
                compValue = avgCompViews.toString(),
                userIsBetter = userMetrics.monthlyViews > avgCompViews,
                isTied = userMetrics.monthlyViews == avgCompViews,
                strings = strings
            )
            ScorecardRow(
                label = strings.seoScoreLabel,
                userValue = "${userMetrics.seoScore}/100",
                compValue = "$avgCompSeo/100",
                userIsBetter = userMetrics.seoScore > avgCompSeo,
                isTied = userMetrics.seoScore == avgCompSeo,
                strings = strings
            )
        }
    }
}

@Composable
private fun ScorecardRow(
    label: String,
    userValue: String,
    compValue: String,
    userIsBetter: Boolean,
    isTied: Boolean,
    strings: AppStrings
) {
    val badgeColor = when {
        isTied -> MaterialTheme.colorScheme.tertiary
        userIsBetter -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
    val badgeText = when {
        isTied -> strings.tiedBadge
        userIsBetter -> strings.leadingBadge
        else -> strings.trailingBadge
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = userValue,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = compValue,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.weight(0.9f)) {
            Surface(
                shape = RoundedCornerShape(50),
                color = badgeColor.copy(alpha = 0.16f)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CompetitorComparisonBarChart(
    businessName: String,
    userMetrics: UserBenchmarkMetrics,
    competitors: List<CompetitorEntry>,
    selectedMetric: BenchmarkChartMetric,
    strings: AppStrings
) {
    data class BarItem(
        val name: String,
        val rawValue: Long,
        val displayValue: String,
        val isUser: Boolean
    )

    val items = remember(businessName, userMetrics, competitors, selectedMetric) {
        val userItem = when (selectedMetric) {
            BenchmarkChartMetric.SEO_SCORE -> BarItem(
                name = "${strings.yourBusinessLabel} ($businessName)",
                rawValue = userMetrics.seoScore.coerceIn(0L, 100L),
                displayValue = "${userMetrics.seoScore}/100",
                isUser = true
            )
            BenchmarkChartMetric.MONTHLY_VIEWS -> BarItem(
                name = "${strings.yourBusinessLabel} ($businessName)",
                rawValue = userMetrics.monthlyViews.coerceAtLeast(0L),
                displayValue = "${userMetrics.monthlyViews}",
                isUser = true
            )
            BenchmarkChartMetric.REVIEWS -> BarItem(
                name = "${strings.yourBusinessLabel} ($businessName)",
                rawValue = userMetrics.reviewCount.coerceAtLeast(0L),
                displayValue = "${userMetrics.reviewCount}",
                isUser = true
            )
            BenchmarkChartMetric.MAP_RANK -> BarItem(
                name = "${strings.yourBusinessLabel} ($businessName)",
                rawValue = (101L - userMetrics.localRank).coerceIn(4L, 100L),
                displayValue = "#${userMetrics.localRank}",
                isUser = true
            )
        }
        val compItems = competitors.take(5).map { comp ->
            when (selectedMetric) {
                BenchmarkChartMetric.SEO_SCORE -> BarItem(
                    name = comp.name,
                    rawValue = comp.seoScore.coerceIn(0L, 100L),
                    displayValue = "${comp.seoScore}/100",
                    isUser = false
                )
                BenchmarkChartMetric.MONTHLY_VIEWS -> BarItem(
                    name = comp.name,
                    rawValue = comp.monthlyViewsEst.coerceAtLeast(0L),
                    displayValue = "${comp.monthlyViewsEst}",
                    isUser = false
                )
                BenchmarkChartMetric.REVIEWS -> BarItem(
                    name = comp.name,
                    rawValue = comp.reviewCount.coerceAtLeast(0L),
                    displayValue = "${comp.reviewCount}",
                    isUser = false
                )
                BenchmarkChartMetric.MAP_RANK -> BarItem(
                    name = comp.name,
                    rawValue = (101L - comp.localRank).coerceIn(4L, 100L),
                    displayValue = "#${comp.localRank}",
                    isUser = false
                )
            }
        }
        listOf(userItem) + compItems
    }

    val maxBar = (items.maxOfOrNull { it.rawValue } ?: 1L).coerceAtLeast(1L).toFloat()
    val userColor = MaterialTheme.colorScheme.primary
    val compColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEach { bar ->
                val barColor: Color = if (bar.isUser) userColor else compColor
                val ratio = (bar.rawValue / maxBar).coerceIn(0.04f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bar.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (bar.isUser) FontWeight.Bold else FontWeight.Medium,
                            color = if (bar.isUser) userColor else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = bar.displayValue,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    ) {
                        drawRoundRect(
                            color = trackColor,
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        drawRoundRect(
                            color = barColor,
                            size = Size(size.width * ratio, size.height),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompetitorComparisonItemCard(
    competitor: CompetitorEntry,
    userMetrics: UserBenchmarkMetrics,
    strings: AppStrings,
    isAnalyzing: Boolean,
    canReplyOrAnalyze: Boolean,
    onAnalyze: () -> Unit,
    onDelete: () -> Unit
) {
    val rankDelta = competitor.localRank - userMetrics.localRank // positive means user ranks better
    val viewsDelta = userMetrics.monthlyViews - competitor.monthlyViewsEst
    val seoDelta = userMetrics.seoScore - competitor.seoScore

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = competitor.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (competitor.website.isNotBlank()) {
                        Text(
                            text = competitor.website,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (canReplyOrAnalyze) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dashboard_delete_comp_${competitor.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove ${competitor.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Competitor raw metrics row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricTag("Rank #${competitor.localRank}", MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "${competitor.rating} (${competitor.reviewCount})",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                MetricTag("${competitor.monthlyViewsEst} views/mo", MaterialTheme.colorScheme.tertiary)
                MetricTag("${strings.seoScoreLabel}: ${competitor.seoScore}/100", MaterialTheme.colorScheme.primary)
            }

            // Head-to-head deltas vs your business
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DeltaChip(
                    label = when {
                        rankDelta > 0 -> "Rank: +$rankDelta ahead"
                        rankDelta < 0 -> "Rank: ${kotlin.math.abs(rankDelta)} behind"
                        else -> "Rank: Tied"
                    },
                    isPositive = rankDelta >= 0
                )
                DeltaChip(
                    label = if (viewsDelta >= 0) "Views: +$viewsDelta/mo" else "Views: $viewsDelta/mo",
                    isPositive = viewsDelta >= 0
                )
                DeltaChip(
                    label = if (seoDelta >= 0) "SEO: +$seoDelta pts" else "SEO: $seoDelta pts",
                    isPositive = seoDelta >= 0
                )
            }

            OutlinedButton(
                onClick = onAnalyze,
                enabled = !isAnalyzing && canReplyOrAnalyze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("dashboard_analyze_comp_${competitor.id}")
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(strings.analyzeCompetitorAi)
            }

            if (competitor.aiGapSummary.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = competitor.aiGapSummary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTag(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DeltaChip(label: String, isPositive: Boolean) {
    val tint = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = tint,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun AddCompetitorDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        website: String,
        rank: Long,
        rating: Double,
        reviewCount: Long,
        monthlyViewsEst: Long,
        seoScore: Long
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var rankText by remember { mutableStateOf("2") }
    var ratingText by remember { mutableStateOf("4.4") }
    var reviewsText by remember { mutableStateOf("85") }
    var viewsText by remember { mutableStateOf("1400") }
    var seoScoreText by remember { mutableStateOf("72") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Track Local Competitor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Competitor Business Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("comp_dialog_name_input")
                )
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website URL (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("comp_dialog_website_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rankText,
                        onValueChange = { rankText = it.filter { c -> c.isDigit() } },
                        label = { Text("Map Rank") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comp_dialog_rank_input")
                    )
                    OutlinedTextField(
                        value = ratingText,
                        onValueChange = { ratingText = it },
                        label = { Text("Rating (0-5)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comp_dialog_rating_input")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = reviewsText,
                        onValueChange = { reviewsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Reviews") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comp_dialog_reviews_input")
                    )
                    OutlinedTextField(
                        value = viewsText,
                        onValueChange = { viewsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Views / mo") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comp_dialog_views_input")
                    )
                }
                OutlinedTextField(
                    value = seoScoreText,
                    onValueChange = { seoScoreText = it.filter { c -> c.isDigit() } },
                    label = { Text("Estimated SEO Score (0-100)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("comp_dialog_seo_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name.trim(),
                            website.trim(),
                            (rankText.toLongOrNull() ?: 3L).coerceIn(1L, 100L),
                            (ratingText.toDoubleOrNull() ?: 4.2).coerceIn(0.0, 5.0),
                            (reviewsText.toLongOrNull() ?: 0L).coerceAtLeast(0L),
                            (viewsText.toLongOrNull() ?: 0L).coerceAtLeast(0L),
                            (seoScoreText.toLongOrNull() ?: 50L).coerceIn(0L, 100L)
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("comp_dialog_save_btn")
            ) {
                Text("Save Competitor")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("comp_dialog_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
