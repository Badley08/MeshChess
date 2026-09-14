package com.karlitodev.meshchess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

// Material 3 Expressive Theme Colors (Neon Blue/Mesh inspired)
val PrimaryNeon = Color(0xFF00D4FF)
val BackgroundDark = Color(0xFF0B132B)
val SurfaceDark = Color(0xFF1C2541)
val AccentTeal = Color(0xFF5BC0BE)

private val Typography = Typography(
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.sp
    )
)

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

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = PrimaryNeon,
                    background = BackgroundDark,
                    surface = SurfaceDark,
                    secondary = AccentTeal
                ),
                typography = Typography
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
}

@Composable
fun MenuScreen(strings: Map<String, String>, onPlayLocal: () -> Unit, onSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = strings["app_name"] ?: "MeshChess", style = MaterialTheme.typography.headlineLarge, color = PrimaryNeon)
        Spacer(modifier = Modifier.height(48.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth().height(80.dp).clickable { onPlayLocal() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = SurfaceDark)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(text = "♟ ${strings["play_local"]}", style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().height(80.dp).clickable { /* TODO: Online */ },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = SurfaceDark)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(text = "🌐 ${strings["play_online"]}", style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        IconButton(onClick = onSettings) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = AccentTeal, modifier = Modifier.size(32.dp))
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Text("← Back") }
        }
        Text(text = strings["settings"] ?: "Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        Text(text = strings["language"] ?: "Language", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(selected = currentLang == "fr", onClick = { onLangChange("fr") }, label = { Text("🇫🇷 FR") })
            FilterChip(selected = currentLang == "en", onClick = { onLangChange("en") }, label = { Text("🇬🇧 EN") })
            FilterChip(selected = currentLang == "es", onClick = { onLangChange("es") }, label = { Text("🇪🇸 ES") })
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = strings["app_description"] ?: "", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = strings["creator_bio"] ?: "", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))
        FilledTonalButton(onClick = { GitHubUpdater(context).checkForUpdates() }) {
            Text(strings["check_updates"] ?: "Check for Updates")
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

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Text("← Quit") }
        }
        
        Text(text = if (uiState.turn == 'w') "White's Turn" else "Black's Turn", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Chessboard
        Column(modifier = Modifier.padding(16.dp).border(2.dp, PrimaryNeon).background(Color.White)) {
            for (r in 7 downTo 0) {
                Row {
                    for (f in 0..7) {
                        val isDark = (r + f) % 2 == 0
                        val bgColor = if (isDark) Color(0xFF769656) else Color(0xFFEEEED2)
                        
                        // Highlight logic
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
            Text(text = "Game Over: ${uiState.status.result}", color = Color.Red, style = MaterialTheme.typography.titleLarge)
            Button(onClick = { viewModel.restartGame() }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Restart Game")
            }
        } else if (uiState.status.inCheck) {
            Text(text = "CHECK!", color = Color.Red, style = MaterialTheme.typography.titleLarge)
        }
    }
}
