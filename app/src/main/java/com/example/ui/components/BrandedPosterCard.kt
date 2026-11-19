package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.i18n.LocalStrings
import com.example.model.BusinessProfile
import com.example.model.SeoPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
private fun rememberLocalFileExists(path: String): Boolean {
    val exists by produceState(initialValue = false, key1 = path) {
        value = if (path.isBlank()) {
            false
        } else {
            withContext(Dispatchers.IO) { File(path).exists() }
        }
    }
    return exists
}

/**
 * Renders the branded post poster.
 * Always embeds the Owner's photo, Service Logo, Post Description,
 * Owner Phone Number, and Business Address irrespective of who performed the work.
 */
@Composable
fun BrandedPosterPreview(
    post: SeoPost,
    profile: BusinessProfile,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val ownerPhotoPath = post.ownerPhotoSnapshotPath.ifBlank { profile.ownerPhotoPath }
    val logoPath = post.serviceLogoSnapshotPath.ifBlank { profile.serviceLogoPath }
    val ownerPhone = post.ownerPhoneSnapshot.ifBlank { profile.ownerPhone }.ifBlank { "Add owner phone" }
    val address = post.addressSnapshot.ifBlank { profile.address }.ifBlank { "Add business address" }

    val hasLogo = rememberLocalFileExists(logoPath)
    val hasPosterImage = rememberLocalFileExists(post.posterImagePath)
    val hasOwnerPhoto = rememberLocalFileExists(ownerPhotoPath)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF06211A),
        border = BorderStroke(1.5.dp, Color(0xFF34D399).copy(alpha = 0.55f)),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF06211A), Color(0xFF0B3B2E), Color(0xFF051A14))
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Top Brand Header: Service Logo + Business Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF34D399)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasLogo) {
                        Image(
                            painter = rememberAsyncImagePainter(File(logoPath)),
                            contentDescription = "Service Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Service Logo Placeholder",
                            tint = Color(0xFF022C22),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.businessName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = profile.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6EE7B7),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF064E3B)
                ) {
                    Text(
                        text = post.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA7F3D0),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. Scene Banner + Embedded Owner Portrait (ALWAYS present regardless of staff)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F766E), Color(0xFF064E3B))
                        )
                    )
            ) {
                if (hasPosterImage) {
                    Image(
                        painter = rememberAsyncImagePainter(File(post.posterImagePath)),
                        contentDescription = "Post Visual Scene",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xB3041E17))
                                )
                            )
                    )
                }

                // Staff / Work performed tag
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xCC022C22)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkOutline,
                            contentDescription = null,
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "By: ${post.workPerformedBy}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFD1FAE5),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Mandatory Owner Photo Frame + Owner Badge
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, Color(0xFFFBBF24), CircleShape)
                            .background(Color(0xFF064E3B)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasOwnerPhoto) {
                            Image(
                                painter = rememberAsyncImagePainter(File(ownerPhotoPath)),
                                contentDescription = "Business Owner Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Owner Photo Placeholder",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFBBF24)
                    ) {
                        Text(
                            text = "${strings.ownerBadgeLabel}: ${profile.ownerName.take(12)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF451A03),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 3. Post Title, Description & CTA Pill
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = post.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD1FAE5),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF34D399)
                ) {
                    Text(
                        text = post.ctaLabel.ifBlank { profile.gmbCtaType },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF022C22),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = post.channels.joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9CA3AF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 4. Mandatory Owner Phone & Address Footer Strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF022C22),
                border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Owner Phone",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ownerPhone,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Business Address",
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
