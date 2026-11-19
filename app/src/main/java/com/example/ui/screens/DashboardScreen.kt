package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.LocalStrings
import com.example.model.AnalyticsLog
import com.example.model.BusinessProfile
import com.example.model.CompetitorEntry
import com.example.model.CustomerReview
import com.example.model.KeywordTarget
import com.example.model.SeoPost

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    profile: BusinessProfile,
    analyticsLogs: List<AnalyticsLog>,
    reviews: List<CustomerReview>,
    keywords: List<KeywordTarget>,
    posts: List<SeoPost>,
    competitors: List<CompetitorEntry>,
    aiBusyCompetitorId: String?,
    isOnline: Boolean,
    canEdit: Boolean,
    canReplyOrAnalyze: Boolean,
    onOpenWelcome: () -> Unit,
    onLogMetric: (leads: Long, calls: Long, views: Long) -> Unit,
    onUpdateGmb: (status: String, url: String, cta: String) -> Unit,
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
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    onSendWhatsappReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var gmbUrlInput by remember(profile.gmbPlaceUrl) { mutableStateOf(profile.gmbPlaceUrl) }
    var selectedCta by remember(profile.gmbCtaType) { mutableStateOf(profile.gmbCtaType) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.businessName,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${profile.category} • ${profile.subscriptionTier} (${profile.billingCycle})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                TextButton(
                                    onClick = onOpenWelcome,
                                    modifier = Modifier.testTag("open_setup_guide_btn")
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = "Setup Guide", modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Setup")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusPill(
                                    label = if (isOnline) "Cloud Synced" else "Offline Cached",
                                    color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                                StatusPill(
                                    label = if (profile.e2eeEnabled) "AES-256 E2EE" else "Standard",
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = strings.monthlySection,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = strings.leadsLabel,
                        value = profile.monthlyLeads,
                        icon = Icons.Default.Leaderboard,
                        accent = MaterialTheme.colorScheme.primary,
                        actionLabel = strings.logLead,
                        enabled = canEdit,
                        onAction = { onLogMetric(1L, 0L, 0L) },
                        testTag = "log_lead_btn",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = strings.callsLabel,
                        value = profile.monthlyCalls,
                        icon = Icons.Default.Call,
                        accent = MaterialTheme.colorScheme.secondary,
                        actionLabel = strings.logCall,
                        enabled = canEdit,
                        onAction = { onLogMetric(0L, 1L, 0L) },
                        testTag = "log_call_btn",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = strings.viewsLabel,
                        value = profile.monthlyViews,
                        icon = Icons.Default.Visibility,
                        accent = MaterialTheme.colorScheme.tertiary,
                        actionLabel = strings.logView,
                        enabled = canEdit,
                        onAction = { onLogMetric(0L, 0L, 10L) },
                        testTag = "log_view_btn",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    text = strings.allTimeSection,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AllTimeStat(strings.leadsLabel, profile.allTimeLeads, MaterialTheme.colorScheme.primary)
                        AllTimeStat(strings.callsLabel, profile.allTimeCalls, MaterialTheme.colorScheme.secondary)
                        AllTimeStat(strings.viewsLabel, profile.allTimeViews, MaterialTheme.colorScheme.tertiary)
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.monthlyTrendTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (analyticsLogs.isEmpty()) {
                            Text(
                                text = "Tap + Lead, + Call, or + View above to record this month's live analytics series.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            AnalyticsBarChart(
                                logs = analyticsLogs,
                                primaryColor = MaterialTheme.colorScheme.primary,
                                secondaryColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                analyticsLogs.takeLast(4).forEach { log ->
                                    Text(
                                        text = "${log.monthLabel}: ${log.leads}L / ${log.calls}C / ${log.views}V",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                CompetitorBenchmarkSection(
                    profile = profile,
                    reviews = reviews,
                    keywords = keywords,
                    posts = posts,
                    competitors = competitors,
                    aiBusyCompetitorId = aiBusyCompetitorId,
                    canEdit = canEdit,
                    canReplyOrAnalyze = canReplyOrAnalyze,
                    onAddCompetitor = onAddCompetitor,
                    onAnalyzeCompetitor = onAnalyzeCompetitor,
                    onDeleteCompetitor = onDeleteCompetitor
                )
            }

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.gmbHubTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val statusText = when (profile.gmbStatus) {
                                "linked" -> strings.gmbStatusLinked
                                "creating" -> strings.gmbStatusCreating
                                else -> strings.gmbStatusNotLinked
                            }
                            StatusPill(statusText, MaterialTheme.colorScheme.primary)
                        }

                        OutlinedTextField(
                            value = gmbUrlInput,
                            onValueChange = { gmbUrlInput = it },
                            label = { Text("Google Business Profile Link / Place URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("gmb_url_input")
                        )

                        Text(
                            text = strings.gmbCtaTitle,
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("CALL", "BOOK", "LEARN_MORE", "ORDER", "SIGN_UP").forEach { cta ->
                                FilterChip(
                                    selected = selectedCta == cta,
                                    onClick = { selectedCta = cta },
                                    label = { Text(cta.replace("_", " ")) },
                                    modifier = Modifier.testTag("gmb_cta_$cta")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onUpdateGmb("creating", gmbUrlInput, selectedCta)
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://business.google.com/create")
                                    )
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                    }
                                },
                                enabled = canEdit,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_create_gmb_btn")
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Create GMB", maxLines = 1)
                            }
                            Button(
                                onClick = { onUpdateGmb("linked", gmbUrlInput, selectedCta) },
                                enabled = canEdit,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dashboard_link_gmb_btn")
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Link Google", maxLines = 1)
                            }
                        }
                    }
                }
            }

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
                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Stakeholder Reports & WhatsApp Delivery",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onExportPdf,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("export_pdf_btn")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.exportPdf)
                            }
                            OutlinedButton(
                                onClick = onExportCsv,
                                modifier = Modifier.weight(1f).height(48.dp).testTag("export_csv_btn")
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.exportCsv)
                            }
                        }
                        Button(
                            onClick = onSendWhatsappReport,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("whatsapp_monthly_report_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.sendMonthlyWhatsapp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: Long,
    icon: ImageVector,
    accent: Color,
    actionLabel: String,
    enabled: Boolean,
    onAction: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onAction,
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(36.dp).testTag(testTag)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
    }
}

@Composable
private fun AllTimeStat(label: String, value: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AnalyticsBarChart(
    logs: List<AnalyticsLog>,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val recent = logs.takeLast(6)
    val maxVal = (recent.maxOfOrNull { maxOf(it.leads, it.calls) } ?: 1L).coerceAtLeast(1L).toFloat()

    Canvas(modifier = modifier) {
        val count = recent.size.coerceAtLeast(1)
        val groupWidth = size.width / count
        val barWidth = (groupWidth * 0.28f).coerceAtLeast(10f)
        val chartHeight = size.height - 16f

        recent.forEachIndexed { idx, log ->
            val cx = idx * groupWidth + groupWidth / 2f
            val leadH = (log.leads / maxVal) * chartHeight
            val callH = (log.calls / maxVal) * chartHeight

            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(cx - barWidth - 4f, size.height - leadH),
                size = Size(barWidth, leadH.coerceAtLeast(6f)),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = secondaryColor,
                topLeft = Offset(cx + 4f, size.height - callH),
                size = Size(barWidth, callH.coerceAtLeast(6f)),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }
    }
}
