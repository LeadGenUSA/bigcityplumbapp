package com.bigcityplumbing.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.bigcityplumbing.R
import com.bigcityplumbing.config.AppConfig
import com.bigcityplumbing.config.Coupon
import com.bigcityplumbing.ui.theme.BrandBlue
import com.bigcityplumbing.ui.theme.BrandBlueDark
import com.bigcityplumbing.ui.theme.BrandOrange
import com.bigcityplumbing.ui.theme.BrandOrangeDark

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { HeaderHero() }

        item {
            // Big call-to-action: Call now
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse(AppConfig.telUri()))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
            ) {
                Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    "Call ${AppConfig.PHONE_NUMBER_DISPLAY}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (AppConfig.OFFERS.isNotEmpty()) {
            item {
                Text(
                    "Special Offers",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            item {
                Text(
                    "Tap a coupon to call and redeem.",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(AppConfig.OFFERS) { offer ->
                        CouponCard(offer) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse(AppConfig.telUri()))
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "What can we help with?",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false,
                contentPadding = PaddingValues(8.dp),
            ) {
                item { TileCard("Service Request", "Schedule a visit", Icons.Outlined.Build) { onNavigate("service") } }
                item { TileCard("Help Guides", "DIY tips & advice", Icons.Outlined.Info) { onNavigate("guides") } }
                item { TileCard("Video Hub", "Watch our latest videos", Icons.Outlined.PlayArrow) { onNavigate("videos") } }
                item { TileCard("Pipe Puzzle", "Quick game break", Icons.Outlined.SportsEsports) { onNavigate("game") } }
            }
        }
    }
}

@Composable
private fun HeaderHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(BrandBlue, BrandBlueDark))
            )
            .padding(24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.big_city_logo),
                contentDescription = "Big City Plumbing logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(116.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                AppConfig.COMPANY_NAME,
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Centereach, NY • Licensed & Insured",
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Plumbing, heating & emergency service.\nTap below to call us 24/7.",
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun CouponCard(offer: Coupon, onRedeem: () -> Unit) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onRedeem() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // Orange "ticket stub" header with the headline discount
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(BrandOrange, BrandOrangeDark)))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    offer.discount,
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    offer.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    offer.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Promo code "chip"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, BrandOrange), RoundedCornerShape(8.dp))
                            .background(BrandOrange.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            offer.code,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandOrangeDark,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = BrandBlue,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Redeem",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBlue,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TileCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = BrandBlue,
                )
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}
