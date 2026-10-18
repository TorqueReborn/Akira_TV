package com.ghostreborn.akira.features.settings.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.home.services.AnimeRepository
import com.ghostreborn.akira.ui.theme.AkiraDarkBackground
import com.ghostreborn.akira.ui.theme.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import kotlinx.coroutines.launch

@Composable
fun TVSettingsScreen(
    onNavigateBack: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var allowAdult by remember { mutableStateOf(AnimeRepository.allowAdult) }
    var denyEcchi by remember { mutableStateOf(AnimeRepository.denyEcchi) }
    var allowUnknown by remember { mutableStateOf(AnimeRepository.allowUnknown) }
    var isLoggingOut by remember { mutableStateOf(false) }

    val username = remember { TokenManager.getUsername(context) ?: "Otaku Member" }
    val email = remember { TokenManager.getEmail(context) }
    val picture = remember { TokenManager.getPicture(context) }

    BackHandler { onNavigateBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AkiraDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                val backInteraction = remember { MutableInteractionSource() }
                val isBackFocused by backInteraction.collectIsFocusedAsState()

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(if (isBackFocused) 2.dp else 0.dp, if (isBackFocused) Color.White else Color.Transparent, CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(interactionSource = backInteraction, indication = null) {
                            onNavigateBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(18.dp))

                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!picture.isNullOrBlank()) {
                    AsyncImage(
                        model = picture,
                        contentDescription = username,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(brush = AkiraLogoGradientBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        text = username,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!email.isNullOrBlank()) {
                        Text(
                            text = email,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "CONTENT PREFERENCES",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            TVSettingsToggle(
                title = "Allow Adult Content (18+)",
                subtitle = "Include R-18 and NSFW titles in feeds and searches",
                checked = allowAdult,
                onCheckedChange = {
                    allowAdult = it
                    AnimeRepository.updateContentPreferences(context, newAllowAdult = it)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            TVSettingsToggle(
                title = "Deny Ecchi Content",
                subtitle = "Filter out fanservice and suggestive anime",
                checked = denyEcchi,
                onCheckedChange = {
                    denyEcchi = it
                    AnimeRepository.updateContentPreferences(context, newDenyEcchi = it)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            TVSettingsToggle(
                title = "Allow Unknown Ratings",
                subtitle = "Display unrated or newly listed series",
                checked = allowUnknown,
                onCheckedChange = {
                    allowUnknown = it
                    AnimeRepository.updateContentPreferences(context, newAllowUnknown = it)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            val logoutInteraction = remember { MutableInteractionSource() }
            val isLogoutFocused by logoutInteraction.collectIsFocusedAsState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(if (isLogoutFocused) 2.dp else 0.dp, if (isLogoutFocused) Color.White else Color.Transparent, RoundedCornerShape(14.dp))
                    .background(if (isLogoutFocused) Color.Red.copy(alpha = 0.8f) else Color.Red.copy(alpha = 0.2f))
                    .clickable(interactionSource = logoutInteraction, indication = null) {
                        isLoggingOut = true
                        scope.launch {
                            val token = TokenManager.getAccessToken(context)
                            AuthRepository.logout(token, context)
                            TokenManager.clear(context)
                            isLoggingOut = false
                            onLogoutSuccess()
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoggingOut) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Logout, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Log Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TVSettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(14.dp))
            .background(if (isFocused) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
            .clickable(interactionSource = interaction, indication = null) {
                onCheckedChange(!checked)
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AkiraPrimary
            )
        )
    }
}
