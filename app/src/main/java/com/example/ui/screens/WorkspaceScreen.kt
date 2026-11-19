package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.example.core.AppLanguage
import com.example.core.ThemeColorPalette
import com.example.core.ThemePreference
import com.example.core.UserPreferences
import com.example.core.WorkspaceRole
import com.example.i18n.LocalStrings
import com.example.model.BusinessProfile
import com.example.model.TeamMember
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceScreen(
    profile: BusinessProfile,
    teamMembers: List<TeamMember>,
    prefs: UserPreferences,
    decryptedVaultNote: String,
    onSaveProfile: (
        bizName: String,
        category: String,
        ownerName: String,
        phone: String,
        address: String,
        website: String,
        desc: String
    ) -> Unit,
    onUploadOwnerPhoto: (Uri) -> Unit,
    onUploadServiceLogo: (Uri) -> Unit,
    onUpdateSubscription: (tier: String, cycle: String, trial: Boolean, paymentLabel: String) -> Unit,
    onSetPreviewRole: (WorkspaceRole) -> Unit,
    onAddTeamMember: (name: String, email: String, role: String) -> Unit,
    onRemoveTeamMember: (String) -> Unit,
    onSaveEncryptedVaultNote: (String) -> Unit,
    onUpdatePrivacy: (gdprConsent: Boolean, e2eeEnabled: Boolean) -> Unit,
    onExportGdpr: () -> Unit,
    onEraseGdpr: () -> Unit,
    onSetTheme: (ThemePreference) -> Unit,
    onSetColorPalette: (ThemeColorPalette) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onTriggerTestAlert: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val canEdit = prefs.activePreviewRole.canEditContent
    val isOwner = prefs.activePreviewRole.canManageBillingAndTeam

    var bizName by remember(profile.businessName) { mutableStateOf(profile.businessName) }
    var category by remember(profile.category) { mutableStateOf(profile.category) }
    var ownerName by remember(profile.ownerName) { mutableStateOf(profile.ownerName) }
    var ownerPhone by remember(profile.ownerPhone) { mutableStateOf(profile.ownerPhone) }
    var address by remember(profile.address) { mutableStateOf(profile.address) }
    var websiteUrl by remember(profile.websiteUrl) { mutableStateOf(profile.websiteUrl) }
    var description by remember(profile.description) { mutableStateOf(profile.description) }

    var selectedCycle by remember(profile.billingCycle) { mutableStateOf(profile.billingCycle) }
    var paymentMethod by remember(profile.paymentMethodLabel) { mutableStateOf(profile.paymentMethodLabel.ifBlank { "UPI / Card Autopay" }) }

    var memberName by remember { mutableStateOf("") }
    var memberEmail by remember { mutableStateOf("") }
    var memberRole by remember { mutableStateOf("MANAGER") }

    var vaultInput by remember(decryptedVaultNote) { mutableStateOf(decryptedVaultNote) }

    val ownerPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onUploadOwnerPhoto(uri)
    }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onUploadServiceLogo(uri)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Business Profile & Owner Media
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
                        Text("Business Owner & Brand Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (profile.ownerPhotoPath.isNotBlank() && File(profile.ownerPhotoPath).exists()) {
                                Image(
                                    painter = rememberAsyncImagePainter(File(profile.ownerPhotoPath)),
                                    contentDescription = "Owner Photo",
                                    modifier = Modifier.size(64.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            if (profile.serviceLogoPath.isNotBlank() && File(profile.serviceLogoPath).exists()) {
                                Image(
                                    painter = rememberAsyncImagePainter(File(profile.serviceLogoPath)),
                                    contentDescription = "Service Logo",
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    ownerPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                enabled = canEdit,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.changeOwnerPhoto, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                enabled = canEdit,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.changeServiceLogo, maxLines = 1)
                            }
                        }
                        OutlinedTextField(value = bizName, onValueChange = { bizName = it }, label = { Text("Business Name") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name") }, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = ownerPhone, onValueChange = { ownerPhone = it }, label = { Text("Owner Phone") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = websiteUrl, onValueChange = { websiteUrl = it }, label = { Text("Website URL") }, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Business Address") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("SEO Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = { onSaveProfile(bizName, category, ownerName, ownerPhone, address, websiteUrl, description) },
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_btn")
                        ) {
                            Text("Save Business Profile")
                        }
                    }
                }
            }

            // 2. Pricing Tiers (Monthly, Half-Yearly, Annual + Free Trial + Payment Gateway)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.pricingTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Current: ${profile.subscriptionTier} (${profile.billingCycle}) • ${if (profile.trialActive) "14-Day Free Trial Active" else profile.paymentMethodLabel}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "MONTHLY" to strings.monthly,
                                "HALF_YEARLY" to strings.halfYearly,
                                "ANNUAL" to strings.annual
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedCycle == key,
                                    onClick = { selectedCycle = key },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("billing_cycle_$key")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = { paymentMethod = it },
                            label = { Text("Payment Gateway / Recurring Mandate (Stripe / Razorpay / UPI)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val priceMultiplier = when (selectedCycle) {
                            "HALF_YEARLY" -> 5
                            "ANNUAL" -> 10
                            else -> 1
                        }
                        val cycleSuffix = when (selectedCycle) {
                            "HALF_YEARLY" -> "/ 6 mo (1 mo free)"
                            "ANNUAL" -> "/ yr (2 mo free)"
                            else -> "/ mo"
                        }

                        PlanCard(
                            tier = "STARTER",
                            price = "$${29 * priceMultiplier} $cycleSuffix",
                            features = "GMB Linking • Weekly Posts • Basic Analytics",
                            isCurrent = profile.subscriptionTier == "STARTER",
                            enabled = isOwner,
                            onSelect = { onUpdateSubscription("STARTER", selectedCycle, false, paymentMethod) }
                        )
                        PlanCard(
                            tier = "GROWTH",
                            price = "$${59 * priceMultiplier} $cycleSuffix",
                            features = "Daily Auto-Post • AI Copy & Keywords • Monthly WhatsApp PDF",
                            isCurrent = profile.subscriptionTier == "GROWTH",
                            enabled = isOwner,
                            onSelect = { onUpdateSubscription("GROWTH", selectedCycle, false, paymentMethod) }
                        )
                        PlanCard(
                            tier = "ENTERPRISE",
                            price = "$${99 * priceMultiplier} $cycleSuffix",
                            features = "14-Day Free Trial • Multi-Location RBAC • AI Logo & Poster Art • Priority Auto-Reply",
                            isCurrent = profile.subscriptionTier == "ENTERPRISE",
                            enabled = isOwner,
                            trialAction = { onUpdateSubscription("ENTERPRISE", selectedCycle, true, "14-Day Enterprise Trial") },
                            trialLabel = strings.startFreeTrial,
                            onSelect = { onUpdateSubscription("ENTERPRISE", selectedCycle, false, paymentMethod) }
                        )
                    }
                }
            }

            // 3. Role-Based Access Control (RBAC)
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
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.rbacTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("Active Permission Mode (Preview RBAC):", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WorkspaceRole.entries.forEach { role ->
                                FilterChip(
                                    selected = prefs.activePreviewRole == role,
                                    onClick = { onSetPreviewRole(role) },
                                    label = { Text(role.label) },
                                    modifier = Modifier.testTag("rbac_role_${role.name}")
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = memberName,
                                onValueChange = { memberName = it },
                                label = { Text("Team Member Name") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = memberEmail,
                                onValueChange = { memberEmail = it },
                                label = { Text("Email") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MANAGER", "EDITOR", "VIEWER").forEach { role ->
                                FilterChip(
                                    selected = memberRole == role,
                                    onClick = { memberRole = role },
                                    label = { Text(role) }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                if (memberName.isNotBlank() && memberEmail.contains("@")) {
                                    onAddTeamMember(memberName, memberEmail, memberRole)
                                    memberName = ""
                                    memberEmail = ""
                                }
                            },
                            enabled = isOwner,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Add Team Member")
                        }
                        teamMembers.forEach { tm ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${tm.memberName} (${tm.memberEmail}) — ${tm.memberRole}", style = MaterialTheme.typography.bodySmall)
                                if (isOwner) {
                                    IconButton(onClick = { onRemoveTeamMember(tm.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove Member", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. End-to-End Encryption & GDPR Compliance
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
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.securityGdprTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.gdprConsentLabel, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = profile.gdprConsent,
                                onCheckedChange = { if (isOwner) onUpdatePrivacy(it, profile.e2eeEnabled) }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("AES-256 E2EE Communications", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = profile.e2eeEnabled,
                                onCheckedChange = { if (isOwner) onUpdatePrivacy(profile.gdprConsent, it) }
                            )
                        }
                        OutlinedTextField(
                            value = vaultInput,
                            onValueChange = { vaultInput = it },
                            label = { Text(strings.e2eeVaultTitle) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { onSaveEncryptedVaultNote(vaultInput) },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Encrypt & Store Locally (Keystore AES-GCM)")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onExportGdpr,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text(strings.exportGdprData, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onEraseGdpr,
                                enabled = isOwner,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("GDPR Erase Data", maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 5. Theme (Dark Mode), Hindi/English Language, Test Alert & Sign Out
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
                        Text(strings.appearanceLanguageTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(strings.darkModeLabel, style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemePreference.entries.forEach { theme ->
                                FilterChip(
                                    selected = prefs.theme == theme,
                                    onClick = { onSetTheme(theme) },
                                    label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    modifier = Modifier.testTag("theme_${theme.name}")
                                )
                            }
                        }

                        Text(strings.themeColorLabel, style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ThemeColorPalette.entries.forEach { palette ->
                                val labelText = if (prefs.language == AppLanguage.HINDI) palette.labelHi else palette.label
                                FilterChip(
                                    selected = prefs.colorPalette == palette,
                                    onClick = { onSetColorPalette(palette) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(palette.previewHex))
                                        )
                                    },
                                    label = { Text(labelText) },
                                    modifier = Modifier.testTag("palette_${palette.name}")
                                )
                            }
                        }

                        Text(strings.languageLabel, style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = prefs.language == lang,
                                    onClick = { onSetLanguage(lang) },
                                    label = { Text(lang.label) },
                                    modifier = Modifier.testTag("lang_${lang.code}")
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onTriggerTestAlert,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send Real-Time System Alert")
                        }

                        Button(
                            onClick = onSignOut,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.signOut)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    tier: String,
    price: String,
    features: String,
    isCurrent: Boolean,
    enabled: Boolean,
    trialAction: (() -> Unit)? = null,
    trialLabel: String = "",
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tier, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(price, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text(features, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (trialAction != null) {
                    OutlinedButton(
                        onClick = trialAction,
                        enabled = enabled,
                        modifier = Modifier.weight(1f).height(44.dp).testTag("trial_${tier}_btn")
                    ) {
                        Text(trialLabel, maxLines = 1)
                    }
                }
                Button(
                    onClick = onSelect,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).height(44.dp).testTag("select_${tier}_btn")
                ) {
                    Text(if (isCurrent) "Active" else "Subscribe", maxLines = 1)
                }
            }
        }
    }
}
