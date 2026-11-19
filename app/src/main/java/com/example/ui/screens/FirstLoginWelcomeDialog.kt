package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.i18n.LocalStrings
import com.example.model.BusinessProfile

@Composable
fun FirstLoginWelcomeDialog(
    profile: BusinessProfile,
    onCreateGmb: (businessName: String, phone: String, address: String) -> Unit,
    onLinkGmb: (businessName: String, phone: String, address: String, gmbUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    var bizName by remember(profile.businessName) { mutableStateOf(profile.businessName) }
    var phone by remember(profile.ownerPhone) { mutableStateOf(profile.ownerPhone) }
    var address by remember(profile.address) { mutableStateOf(profile.address) }
    var gmbUrl by remember(profile.gmbPlaceUrl) { mutableStateOf(profile.gmbPlaceUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = strings.welcomeTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.welcomeSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                BenefitCard(strings.welcomeStep1Title, strings.welcomeStep1Desc)
                BenefitCard(strings.welcomeStep2Title, strings.welcomeStep2Desc)
                BenefitCard(strings.welcomeStep3Title, strings.welcomeStep3Desc)

                OutlinedTextField(
                    value = bizName,
                    onValueChange = { bizName = it },
                    label = { Text("Business Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("welcome_biz_name")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Owner Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("welcome_phone")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Business Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("welcome_address")
                )
                OutlinedTextField(
                    value = gmbUrl,
                    onValueChange = { gmbUrl = it },
                    label = { Text("Google Business Profile URL (if linking)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("welcome_gmb_url")
                )

                // First option: Create Google Business Profile OR Link to Google
                Button(
                    onClick = {
                        onCreateGmb(bizName, phone, address)
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://business.google.com/create")
                        )
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("welcome_create_gmb_button")
                ) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.createGmbOption)
                }

                OutlinedButton(
                    onClick = { onLinkGmb(bizName, phone, address, gmbUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("welcome_link_gmb_button")
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.linkGmbOption)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("welcome_continue_button")
                ) {
                    Text(strings.continueToDashboard)
                }
            }
        }
    }
}

@Composable
private fun BenefitCard(title: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
