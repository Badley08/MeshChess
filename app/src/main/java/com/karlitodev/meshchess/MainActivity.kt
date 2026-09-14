package com.karlitodev.meshchess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.karlitodev.meshchess.engine.GameViewModel
import com.karlitodev.meshchess.language.en.EnglishUI
import com.karlitodev.meshchess.language.es.EspagnolUI
import com.karlitodev.meshchess.language.fr.FrenchUI

// Skeuomorphic Theme Colors
val BgTealLight = Color(0xFF0F4A4A)
val BgTealDark = Color(0xFF041E1E)
val WoodLight = Color(0xFF6B3E12)
val WoodDark = Color(0xFF3E1F03)
val RibbonRed = Color(0xFFC70000)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsManager = SettingsManager(this)

        setContent {
            var selectedLang by remember { mutableStateOf(settingsManager.getLanguage()) }
            val strings = when (selectedLang) {
                "en" -> EnglishUI.strings
                "es" -> EspagnolUI.strings
                else -> FrenchUI.strings
            }

            MaterialTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "menu") {
                    composable("menu") {
                        MenuScreen(
                            strings = strings,
                            onPlayLocal = { navController.navigate("game_local") },
                            onSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("game_local") {
                        val viewModel: GameViewModel = viewModel()
                        GameScreen(
                            viewModel = viewModel,
                            strings = strings,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            strings = strings,
                            currentLang = selectedLang,
                            onLangChange = {
                                selectedLang = it
                                settingsManager.saveLanguage(it)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MenuScreen(strings: Map<String, String>, onPlayLocal: () -> Unit, onSettings: () -> Unit) {
    val bgBrush = Brush.radialGradient(
        colors = listOf(BgTealLight, BgTealDark),
        radius = 1500f
    )

    Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Wood Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Brush.verticalGradient(listOf(WoodLight, WoodDark)))
                    .border(2.dp, Color(0xFFD4AF37)) // Gold trim
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Profile Pic (karlito.png)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .border(3.dp, Color(0xFFFFD700), CircleShape) // Gold border
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.karlito),
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Settings Ribbon
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(80.dp)
                        .background(RibbonRed)
                        .clickable { onSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Main Logo (mesh_chess.png)
            Image(
                painter = painterResource(id = R.drawable.mesh_chess),
                contentDescription = "App Logo",
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "CHOOSE YOUR MODE",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            GlossyButton(
                text = "PLAY VS COMPUTER",
                icon = "🤖",
                isGreen = true,
                onClick = { /* TODO */ }
            )
            Spacer(modifier = Modifier.height(16.dp))
            GlossyButton(
                text = "2 PLAYERS",
                icon = "👥",
                isGreen = false,
                onClick = onPlayLocal
            )
            Spacer(modifier = Modifier.height(16.dp))
            GlossyButton(
                text = "PUZZLES",
                icon = "🧩",
                isGreen = false,
                onClick = { /* TODO */ }
            )
            Spacer(modifier = Modifier.height(16.dp))
            GlossyButton(
                text = "LEARN CHESS",
                icon = "🎓",
                isGreen = false,
                onClick = { /* TODO */ }
            )
        }
    }
}

@Composable
fun GlossyButton(text: String, icon: String, isGreen: Boolean, onClick: () -> Unit) {
    val topColor = if (isGreen) Color(0xFF6DE815) else Color(0xFF4AC4E7)
    val bottomColor = if (isGreen) Color(0xFF1E8A08) else Color(0xFF086A8A)
    
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(64.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
                shape = RoundedCornerShape(12.dp)
            )
            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp)) // Highlights
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsScreen(
    strings: Map<String, String>,
    currentLang: String,
    onLangChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().background(BgTealDark).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Text("← Back", color = Color.White) }
        }
        Text(text = strings["settings"] ?: "Settings", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = strings["language"] ?: "Language", color = Color.LightGray)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(selected = currentLang == "fr", onClick = { onLangChange("fr") }, label = { Text("🇫🇷 FR") })
            FilterChip(selected = currentLang == "en", onClick = { onLangChange("en") }, label = { Text("🇬🇧 EN") })
            FilterChip(selected = currentLang == "es", onClick = { onLangChange("es") }, label = { Text("🇪🇸 ES") })
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { GitHubUpdater(context).checkForUpdates() },
            colors = ButtonDefaults.buttonColors(containerColor = RibbonRed)
        ) {
            Text(strings["check_updates"] ?: "Check for Updates", color = Color.White)
        }
    }
}

@Composable
fun GameScreen(viewModel: GameViewModel, strings: Map<String, String>, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val unicodePieces = mapOf(
        "wK" to "♔", "wQ" to "♕", "wR" to "♖", "wB" to "♗", "wN" to "♘", "wP" to "♙",
        "bK" to "♚", "bQ" to "♛", "bR" to "♜", "bB" to "♝", "bN" to "♞", "bP" to "♟"
    )

    Column(modifier = Modifier.fillMaxSize().background(BgTealDark), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Text("← Quit", color = Color.White) }
        }
        
        Text(
            text = if (uiState.turn == 'w') "White's Turn" else "Black's Turn",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(16.dp).border(4.dp, WoodLight).background(Color.White)) {
            for (r in 7 downTo 0) {
                Row {
                    for (f in 0..7) {
                        val isDark = (r + f) % 2 == 0
                        val bgColor = if (isDark) Color(0xFF769656) else Color(0xFFEEEED2)
                        
                        val isSelected = uiState.selectedSquare?.file == f && uiState.selectedSquare?.rank == r
                        val isMoveTarget = uiState.legalMovesForSelected.any { it.to.file == f && it.to.rank == r }
                        
                        val finalBgColor = when {
                            isSelected -> Color(0xFFF6F669)
                            isMoveTarget -> Color(0xFFD42C2C).copy(alpha = 0.5f)
                            else -> bgColor
                        }

                        Box(
                            modifier = Modifier.size(44.dp).background(finalBgColor).clickable { viewModel.onSquareClicked(f, r) },
                            contentAlignment = Alignment.Center
                        ) {
                            val piece = uiState.board[r][f]
                            if (piece != null) {
                                Text(text = unicodePieces[piece] ?: "", fontSize = 32.sp, color = if (piece[0] == 'w') Color.White else Color.Black)
                            }
                            if (isMoveTarget && piece == null) {
                                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.2f)))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (uiState.status.gameOver) {
            Text(text = "Game Over: ${uiState.status.result}", color = Color.Red, fontSize = 24.sp)
            Button(onClick = { viewModel.restartGame() }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Restart Game")
            }
        } else if (uiState.status.inCheck) {
            Text(text = "CHECK!", color = Color.Red, fontSize = 24.sp)
        }
    }
}
