package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.RoyalBlue800
import com.example.ui.theme.RoyalBlue900

@Composable
fun AsyncOrLocalImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    when {
        imageUrl == "drawable:dev_port" -> {
            Image(
                painter = painterResource(id = R.drawable.dev_port),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageUrl == "drawable:dev_power" -> {
            Image(
                painter = painterResource(id = R.drawable.dev_power),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageUrl == "drawable:dev_road" -> {
            Image(
                painter = painterResource(id = R.drawable.dev_road),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageUrl == "drawable:dev_kutubdia" -> {
            Image(
                painter = painterResource(id = R.drawable.dev_kutubdia),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageUrl == "drawable:dev_ez" -> {
            Image(
                painter = painterResource(id = R.drawable.dev_ez),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageUrl == "drawable:dev_edu" -> {
            Image(
                painter = painterResource(id = R.drawable.dev_edu),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageUrl.contains("banner") || imageUrl.startsWith("drawable:moheshkhali_banner") -> {
            Image(
                painter = painterResource(id = R.drawable.banner_video_poster),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageUrl.contains("1800") || imageUrl.startsWith("drawable:asheq_profile") || imageUrl.startsWith("drawable:asheq_avatar") -> {
            Image(
                painter = painterResource(id = R.drawable.asheq_profile_custom),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageUrl.contains("1176") || imageUrl.startsWith("drawable:ic_app_icon") -> {
            Image(
                painter = painterResource(id = R.drawable.ic_app_icon_art),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageUrl.startsWith("file://") || imageUrl.startsWith("/") -> {
            val filePath = imageUrl.removePrefix("file://")
            val localFile = java.io.File(filePath)
            if (localFile.exists() && localFile.length() > 0L) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(localFile)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale,
                    error = painterResource(id = R.drawable.asheq_profile_custom)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.asheq_profile_custom),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale
                )
            }
        }
        imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                error = painterResource(id = R.drawable.moheshkhali_banner)
            )
        }
        else -> {
            if (imageUrl.isNotBlank() && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://") || imageUrl.startsWith("/"))) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale,
                    error = painterResource(id = R.drawable.moheshkhali_banner),
                    placeholder = painterResource(id = R.drawable.moheshkhali_banner)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.moheshkhali_banner),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale
                )
            }
        }
    }
}
