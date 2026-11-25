package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.components.AsyncOrLocalImage
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    language: AppLanguage,
    onSplashFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1600) // 1.6 seconds professional splash display
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        RoyalBlue900,
                        RoyalBlue800,
                        Color(0xFF00695C)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // App Icon with glowing circular border & shadow
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(16.dp, CircleShape, spotColor = GoldAccent)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.5.dp, GoldAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncOrLocalImage(
                    imageUrl = "drawable:ic_app_icon_art",
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (language == AppLanguage.BN) "আশেক উল্লাহ রফিক" else "Asheq Ullah Rafiq",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (language == AppLanguage.BN) "উন্নয়ন ও জনসংযোগ পোর্টাল" else "Development & Public Relations Portal",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = GoldAccent,
                strokeWidth = 3.dp
            )
        }
    }
}
