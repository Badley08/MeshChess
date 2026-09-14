package com.karlitodev.meshchess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.karlitodev.meshchess.engine.GeminiAgent
import com.karlitodev.meshchess.language.en.EnglishUI
import com.karlitodev.meshchess.language.es.EspagnolUI
import com.karlitodev.meshchess.language.fr.FrenchUI
import com.karlitodev.meshchess.ui.OnboardingScreen

// Theme Colors
val BgTealLight = Color(0xFF0F4A4A)
val BgTealDark = Color(0xFF041E1E)
val WoodLight = Color(0xFF6B3E12)
val WoodDark = Color(0xFF3E1F03)
val RibbonRed = Color(0xFFC70000)
val GoldTrim = Color(0xFFD4AF37)
val GreenTop = Color(0xFF6DE815)
val GreenBottom = Color(0xFF1E8A08)
val BlueTop = Color(0xFF4AC4E7)
val BlueBottom = Color(0xFF086A8A)

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
                val startDest = if (settingsManager.isFirstLaunch()) "onboarding" else "menu"

                NavHost(navController = navController, startDestination = startDest) {
                    composable("onboarding") {
                        OnboardingScreen(
                            settingsManager = settingsManager,
                            onComplete = {
                                selectedLang = settingsManager.getLanguage()
                                navController.navigate("menu") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("menu") {
                        MenuScreen(
                            strings = strings,
                            onPlayLocal = { navController.navigate("game_local") },
                            onPlayAI = { navController.navigate("game_ai") },
                            onPlayOnline = { navController.navigate("online_lobby") },
                            onSettings = { navController.navigate("settings") }
                        )
                    }

                    composable("game_local") {
                        val vm: GameViewModel = viewModel()
                        LaunchedEffect(Unit) { vm.setGeminiAgent(null) }
                        GameScreen(vm, strings, isLocalMultiplayer = true, onBack = { navController.popBackStack() })
                    }

                    composable("game_ai") {
                        val vm: GameViewModel = viewModel()
                        val apiKey = settingsManager.getGeminiApiKey()
                        val difficulty = settingsManager.getAIDifficulty()
                        LaunchedEffect(Unit) {
                            if (!apiKey.isNullOrBlank()) {
                                vm.setGeminiAgent(GeminiAgent(apiKey, selectedLang, difficulty))
                            }
                        }
                        if (apiKey.isNullOrBlank()) {
                            NoApiKeyScreen(
                                strings = strings,
                                onGoSettings = { navController.navigate("settings") }
                            )
                        } else {
                            GameScreen(vm, strings, isLocalMultiplayer = false, onBack = { navController.popBackStack() })
                        }
                    }

                    composable("online_lobby") {
                        OnlineLobbyScreen(
                            strings = strings,
                            serverUrl = "https://meshchess-server-production.up.railway.app",
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            strings = strings,
                            settingsManager = settingsManager,
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

// ──────────────────────────────────────────────────────────────
//  Menu Screen
// ──────────────────────────────────────────────────────────────
@Composable
fun MenuScreen(
    strings: Map<String, String>,
    onPlayLocal: () -> Unit,
    onPlayAI: () -> Unit,
    onPlayOnline: () -> Unit,
    onSettings: () -> Unit
) {
    val bgBrush = Brush.radialGradient(listOf(BgTealLight, BgTealDark), radius = 1500f)

    Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Wood Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth().height(80.dp)
                    .background(Brush.verticalGradient(listOf(WoodLight, WoodDark)))
                    .border(2.dp, GoldTrim)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.size(60.dp)
                        .border(3.dp, Color(0xFFFFD700), CircleShape)
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.karlito),
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier.width(48.dp).height(80.dp)
                        .background(RibbonRed).clickable { onSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, "Settings", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.mesh_chess),
                contentDescription = "MeshChess",
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("CHOOSE YOUR MODE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // Buttons with XML vector icons
            GlossyButton("PLAY VS COMPUTER", R.drawable.ic_computer, isGreen = true, onClick = onPlayAI)
            Spacer(modifier = Modifier.height(14.dp))
            GlossyButton("2 PLAYERS", R.drawable.ic_two_players, isGreen = false, onClick = onPlayLocal)
            Spacer(modifier = Modifier.height(14.dp))
            GlossyButton("PLAY ONLINE", R.drawable.ic_online, isGreen = false, onClick = onPlayOnline)
            Spacer(modifier = Modifier.height(14.dp))
            GlossyButton("PUZZLES", R.drawable.ic_puzzles, isGreen = false, onClick = { /* TODO */ })
            Spacer(modifier = Modifier.height(14.dp))
            GlossyButton("LEARN CHESS", R.drawable.ic_learn, isGreen = false, onClick = { /* TODO */ })
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  Glossy Button (uses XML drawable icon instead of emoji)
// ──────────────────────────────────────────────────────────────
@Composable
fun GlossyButton(text: String, iconRes: Int, isGreen: Boolean, onClick: () -> Unit) {
    val topColor = if (isGreen) GreenTop else BlueTop
    val bottomColor = if (isGreen) GreenBottom else BlueBottom

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f).height(60.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)), RoundedCornerShape(12.dp))
            .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  No API Key Screen
// ──────────────────────────────────────────────────────────────
@Composable
fun NoApiKeyScreen(strings: Map<String, String>, onGoSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(BgTealDark), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(painterResource(R.drawable.ic_computer), null, tint = Color.White, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                strings["api_key_required"] ?: "API Key Required",
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                strings["api_key_desc"] ?: "To play against the computer, enter your Gemini API key in Settings.",
                color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            GlossyButton(strings["go_settings"] ?: "Go to Settings", R.drawable.settings, isGreen = true, onClick = onGoSettings)
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  Settings Screen
// ──────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    strings: Map<String, String>,
    settingsManager: SettingsManager,
    currentLang: String,
    onLangChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(settingsManager.getGeminiApiKey() ?: "") }

    Column(
        modifier = Modifier.fillMaxSize().background(BgTealDark).padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Text("< ${strings["back"] ?: "Back"}", color = Color.White) }
        }
        Text(strings["settings"] ?: "Settings", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        // Language
        Text(strings["language"] ?: "Language", color = Color.LightGray)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(selected = currentLang == "fr", onClick = { onLangChange("fr") }, label = { Text("FR") })
            FilterChip(selected = currentLang == "en", onClick = { onLangChange("en") }, label = { Text("EN") })
            FilterChip(selected = currentLang == "es", onClick = { onLangChange("es") }, label = { Text("ES") })
        }

        Spacer(modifier = Modifier.height(24.dp))

        // AI Difficulty
        var aiDifficulty by remember { mutableStateOf(settingsManager.getAIDifficulty()) }
        Text(strings["ai_difficulty_label"] ?: "Gemini AI Difficulty", color = Color.LightGray)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(
                selected = aiDifficulty == "easy",
                onClick = { aiDifficulty = "easy"; settingsManager.saveAIDifficulty("easy") },
                label = { Text("Easy") }
            )
            FilterChip(
                selected = aiDifficulty == "medium",
                onClick = { aiDifficulty = "medium"; settingsManager.saveAIDifficulty("medium") },
                label = { Text("Medium") }
            )
            FilterChip(
                selected = aiDifficulty == "hard",
                onClick = { aiDifficulty = "hard"; settingsManager.saveAIDifficulty("hard") },
                label = { Text("Hard") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // API Key
        Text(strings["api_key_label"] ?: "Gemini API Key", color = Color.LightGray)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; settingsManager.saveGeminiApiKey(it) },
            label = { Text("API Key", color = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                focusedBorderColor = BlueTop, unfocusedBorderColor = Color.Gray, cursorColor = BlueTop
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Update button
        GlossyButton(
            text = strings["check_updates"] ?: "Check for Updates",
            iconRes = R.drawable.ic_update,
            isGreen = false,
            onClick = { GitHubUpdater(context).checkForUpdates() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // App version
        Text("v1.0.3", color = Color.Gray, fontSize = 12.sp)
    }
}

// ──────────────────────────────────────────────────────────────
//  Online Lobby Screen (HTTP REST with Railway server)
// ──────────────────────────────────────────────────────────────
@Composable
fun OnlineLobbyScreen(strings: Map<String, String>, serverUrl: String, onBack: () -> Unit) {
    var roomCode by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(BgTealDark), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                TextButton(onClick = onBack) { Text("< ${strings["back"] ?: "Back"}", color = Color.White) }
            }

            Icon(painterResource(R.drawable.ic_online), null, tint = Color.White, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("ONLINE PLAY", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = roomCode,
                onValueChange = { roomCode = it },
                label = { Text(strings["room_code"] ?: "Room Code", color = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = BlueTop, unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            GlossyButton("CREATE ROOM", R.drawable.ic_online, isGreen = true, onClick = {
                status = strings["creating_room"] ?: "Creating room..."
                // TODO: POST to $serverUrl/room/create
            })
            Spacer(modifier = Modifier.height(12.dp))
            GlossyButton("JOIN ROOM", R.drawable.ic_two_players, isGreen = false, onClick = {
                if (roomCode.isNotBlank()) {
                    status = strings["joining_room"] ?: "Joining room..."
                    // TODO: POST to $serverUrl/room/join
                }
            })

            if (status.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(status, color = Color.LightGray, fontSize = 14.sp)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  Game Screen
// ──────────────────────────────────────────────────────────────
@Composable
fun GameScreen(viewModel: GameViewModel, strings: Map<String, String>, isLocalMultiplayer: Boolean, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val unicodePieces = mapOf(
        "wK" to "♔", "wQ" to "♕", "wR" to "♖", "wB" to "♗", "wN" to "♘", "wP" to "♙",
        "bK" to "♚", "bQ" to "♛", "bR" to "♜", "bB" to "♝", "bN" to "♞", "bP" to "♟"
    )

    val isFlipped = isLocalMultiplayer && uiState.turn == 'b'
    val boardRotation = if (isFlipped) 180f else 0f

    // Pawn Promotion Dialog
    if (uiState.pendingPromotionFromTo != null) {
        AlertDialog(
            onDismissRequest = { /* forces player choice */ },
            title = {
                Text(
                    strings["promote_pawn"] ?: "Pawn Promotion",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val turnCol = uiState.turn
                    val promoOptions = listOf(
                        'Q' to (if (turnCol == 'w') "♕" else "♛"),
                        'R' to (if (turnCol == 'w') "♖" else "♜"),
                        'B' to (if (turnCol == 'w') "♗" else "♝"),
                        'N' to (if (turnCol == 'w') "♘" else "♞")
                    )
                    for ((code, symbol) in promoOptions) {
                        Card(
                            modifier = Modifier
                                .size(58.dp)
                                .clickable { viewModel.onPromotionSelected(code) },
                            colors = CardDefaults.cardColors(containerColor = BlueTop),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(symbol, fontSize = 32.sp, color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = BgTealDark
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BgTealDark),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Text("< ${strings["quit"] ?: "Quit"}", color = Color.White) }
        }

        val turnText = if (uiState.turn == 'w') (strings["white_turn"] ?: "White's Turn")
                       else (strings["black_turn"] ?: "Black's Turn")
        Text(
            text = if (uiState.aiIsThinking) (strings["ai_thinking"] ?: "AI is thinking...") else turnText,
            color = if (uiState.aiIsThinking) BlueTop else Color.White,
            fontSize = 22.sp, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Large Responsive Chessboard
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .aspectRatio(1f)
                .border(4.dp, WoodLight)
                .background(WoodDark)
                .rotate(boardRotation)
        ) {
            val squareSize = maxWidth / 8
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 7 downTo 0) {
                    Row(modifier = Modifier.fillMaxWidth().height(squareSize)) {
                        for (f in 0..7) {
                            val isDark = (r + f) % 2 == 0
                            val bgColor = if (isDark) Color(0xFF769656) else Color(0xFFEEEED2)
                            val isSelected = uiState.selectedSquare?.file == f && uiState.selectedSquare?.rank == r
                            val isMoveTarget = uiState.legalMovesForSelected.any { it.to.file == f && it.to.rank == r }
                            val finalBgColor = when {
                                isSelected -> Color(0xFFF6F669)
                                isMoveTarget -> Color(0xFFD42C2C).copy(alpha = 0.55f)
                                else -> bgColor
                            }
                            Box(
                                modifier = Modifier
                                    .width(squareSize)
                                    .height(squareSize)
                                    .background(finalBgColor)
                                    .clickable { viewModel.onSquareClicked(f, r) },
                                contentAlignment = Alignment.Center
                            ) {
                                val piece = uiState.board[r][f]
                                if (piece != null) {
                                    val isWhitePiece = piece[0] == 'w'
                                    if (isWhitePiece) {
                                        // High contrast backdrop pill/badge for white pieces on light squares
                                        Box(
                                            modifier = Modifier
                                                .size(squareSize * 0.82f)
                                                .clip(CircleShape)
                                                .background(Color(0xFF262626).copy(alpha = 0.45f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = unicodePieces[piece] ?: "",
                                                fontSize = (squareSize.value * 0.65f).sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.rotate(if (isFlipped) 180f else 0f)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = unicodePieces[piece] ?: "",
                                            fontSize = (squareSize.value * 0.68f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111111),
                                            modifier = Modifier.rotate(if (isFlipped) 180f else 0f)
                                        )
                                    }
                                }
                                if (isMoveTarget && piece == null) {
                                    Box(
                                        modifier = Modifier
                                            .size(squareSize * 0.32f)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (uiState.status.gameOver) {
            Text("${strings["game_over"] ?: "Game Over"}: ${uiState.status.result}",
                color = Color.Red, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            GlossyButton(strings["play_again"] ?: "Play Again", R.drawable.replay, isGreen = true, onClick = { viewModel.restartGame() })
        } else if (uiState.status.inCheck) {
            Text(strings["check"] ?: "CHECK!", color = Color.Red, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}
