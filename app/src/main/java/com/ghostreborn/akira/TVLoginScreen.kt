package com.ghostreborn.akira

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostreborn.akira.features.auth.services.AuthRepository
import com.ghostreborn.akira.features.auth.services.TokenManager
import com.ghostreborn.akira.features.auth.widgets.CloudflareVerificationDialog
import com.ghostreborn.akira.ui.theme.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.components.AkiraTVButton
import com.ghostreborn.akira.ui.theme.AkiraDarkBackground
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import kotlinx.coroutines.launch

@Composable
fun TVLoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCaptchaDialog by remember { mutableStateOf(false) }

    fun performLogin(turnstileToken: String) {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val authResult = AuthRepository.authenticate(
                    usernameOrEmail = email.trim(),
                    rawPassword = password,
                    recaptchCode = turnstileToken,
                    context = context
                )

                TokenManager.saveAuthData(
                    context = context,
                    accessToken = authResult.accessToken,
                    refreshToken = authResult.refreshToken,
                    sessionId = authResult.sessionId,
                    username = authResult.username,
                    displayName = authResult.displayName,
                    picture = authResult.picture,
                    userId = authResult.userId,
                    email = authResult.email,
                    isEmailVerified = authResult.isEmailVerified
                )

                isLoading = false
                onLoginSuccess()
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.localizedMessage ?: "Login failed. Please check credentials."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AkiraDarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .width(820.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "AKIRA",
                    style = TextStyle(
                        brush = AkiraLogoGradientBrush,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Android TV Cinema Edition",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Stream thousands of anime episodes directly on your big screen with remote navigation.",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(36.dp))

            Column(
                modifier = Modifier.weight(1.1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TVRemoteInputField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Username or Email",
                    icon = Icons.Rounded.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                TVRemoteInputField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    icon = Icons.Rounded.Lock,
                    isPassword = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AkiraTVButton(
                        text = "Sign In",
                        loading = isLoading,
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter both credentials"
                            } else {
                                showCaptchaDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    AkiraTVButton(
                        text = "Guest Mode",
                        isPrimary = false,
                        onClick = { onLoginSuccess() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (showCaptchaDialog) {
            CloudflareVerificationDialog(
                onTokenExtracted = { token ->
                    showCaptchaDialog = false
                    performLogin(token)
                },
                onDismiss = { showCaptchaDialog = false }
            )
        }
    }
}

@Composable
fun TVRemoteInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) AkiraPrimary else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                interactionSource = interaction,
                singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = placeholder
                    },
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}
