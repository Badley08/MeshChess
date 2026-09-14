package com.karlitodev.meshchess

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.karlitodev.meshchess.engine.ChessEngine
import com.karlitodev.meshchess.engine.GameViewModel
import com.karlitodev.meshchess.engine.GeminiAgent
import com.karlitodev.meshchess.engine.Puzzle
import com.karlitodev.meshchess.engine.PuzzleLibrary
import com.karlitodev.meshchess.language.en.EnglishUI
import com.karlitodev.meshchess.language.es.EspagnolUI
import com.karlitodev.meshchess.language.fr.FrenchUI
import com.karlitodev.meshchess.network.WebSocketManager
import com.karlitodev.meshchess.network.WsEvent
import com.karlitodev.meshchess.ui.OnboardingScreen
import kotlinx.coroutines.launch

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
                            onPuzzles = { navController.navigate("puzzles") },
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
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("puzzles") {
                        PuzzleListScreen(
                            strings = strings,
                            lang = selectedLang,
                            onPuzzleSelected = { puzzleId ->
                                navController.navigate("puzzle_game/$puzzleId")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("puzzle_game/{puzzleId}") { backStackEntry ->
                        val puzzleId = backStackEntry.arguments?.getString("puzzleId")?.toIntOrNull() ?: 1
                        PuzzleGameScreen(
                            puzzleId = puzzleId,
                            strings = strings,
                            lang = selectedLang,
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
    onPuzzles: () -> Unit,
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
            GlossyButton("PUZZLES", R.drawable.ic_puzzles, isGreen = false, onClick = onPuzzles)
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
//  Settings Screen (with About Me section)
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

        Spacer(modifier = Modifier.height(24.dp))

        // GitHub Token (for Private Repo Updates)
        var githubToken by remember { mutableStateOf(settingsManager.getGitHubToken() ?: "") }
        Text(strings["github_token_label"] ?: "GitHub Token (PAT - Private Repo)", color = Color.LightGray)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = githubToken,
            onValueChange = { githubToken = it; settingsManager.saveGitHubToken(it) },
            label = { Text("GitHub Token (ghp_...)", color = Color.Gray) },
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
            onClick = { GitHubUpdater(context, githubToken).checkForUpdates() }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── About the Creator ──
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            strings["about_creator"] ?: "About the Creator",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Creator Photo
        Box(
            modifier = Modifier
                .size(100.dp)
                .border(3.dp, GoldTrim, CircleShape)
                .clip(CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.karlito),
                contentDescription = "Karlito",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            strings["creator_name_full"] ?: "Luberisse Karl Brad",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "${strings["creator_alias_label"] ?: "Alias"}: Karlito",
            color = GoldTrim,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            strings["creator_age_label"] ?: "17 years old",
            color = Color.LightGray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            strings["creator_bio"] ?: "",
            color = Color.LightGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.9f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // GitHub link
        TextButton(onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Badley08/MeshChess"))
            context.startActivity(intent)
        }) {
            Text(
                "🔗 ${strings["creator_github"] ?: "View on GitHub"}",
                color = BlueTop,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App version
        Text("v1.0.5", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ──────────────────────────────────────────────────────────────
//  Online Lobby Screen (WebSocket with Railway server)
// ──────────────────────────────────────────────────────────────
@Composable
fun OnlineLobbyScreen(strings: Map<String, String>, onBack: () -> Unit) {
    var roomCode by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("Player") }
    var status by remember { mutableStateOf("") }
    var gameStarted by remember { mutableStateOf(false) }
    var myColor by remember { mutableStateOf("white") }
    var opponentName by remember { mutableStateOf("") }
    var currentFen by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf(listOf<String>()) }
    var chatInput by remember { mutableStateOf("") }
    var drawOffered by remember { mutableStateOf(false) }
    var gameOverMessage by remember { mutableStateOf<String?>(null) }

    val wsManager = remember { WebSocketManager() }
    val engine = remember { ChessEngine() }
    val scope = rememberCoroutineScope()

    // Connect WebSocket on launch
    LaunchedEffect(Unit) {
        wsManager.connect()
    }

    // Collect WebSocket events
    LaunchedEffect(wsManager) {
        wsManager.events.collect { event ->
            when (event) {
                is WsEvent.Connected -> status = strings["online_connecting"]?.replace("...", " ✅") ?: "Connected"
                is WsEvent.RoomJoined -> {
                    myColor = event.color
                    status = if (event.playersCount < 2) {
                        strings["online_waiting_room"] ?: "Waiting for 2nd player..."
                    } else {
                        strings["game_started"] ?: "Game started!"
                    }
                }
                is WsEvent.GameStart -> {
                    opponentName = event.opponentUsername
                    currentFen = event.fen
                    engine.loadFen(event.fen)
                    gameStarted = true
                    status = strings["game_started"] ?: "Game started!"
                }
                is WsEvent.OpponentMoved -> {
                    engine.loadFen(event.fen)
                    currentFen = event.fen
                }
                is WsEvent.PlayerDisconnected -> {
                    status = event.message
                }
                is WsEvent.GameOver -> {
                    val reason = event.reason
                    val winner = event.winner
                    gameOverMessage = when (reason) {
                        "checkmate" -> "Checkmate! ${winner ?: ""} wins!"
                        "resignation" -> "${strings["resigned_msg"] ?: "Resigned"} — ${winner ?: ""} wins!"
                        "draw_agreement" -> "Draw by agreement"
                        "stalemate" -> "Stalemate — Draw"
                        else -> "Game Over: $reason"
                    }
                }
                is WsEvent.QuickMatchWaiting -> {
                    status = strings["online_waiting"] ?: "Waiting for an opponent..."
                }
                is WsEvent.QuickMatchCancelled -> {
                    status = ""
                }
                is WsEvent.DrawOffered -> {
                    drawOffered = true
                }
                is WsEvent.DrawDeclined -> {
                    drawOffered = false
                    chatMessages = chatMessages + (strings["draw_declined_msg"] ?: "Draw declined")
                }
                is WsEvent.ChatMessage -> {
                    chatMessages = chatMessages + "${event.username}: ${event.text}"
                }
                is WsEvent.Error -> {
                    status = "❌ ${event.message}"
                }
                is WsEvent.Disconnected -> {
                    if (!gameStarted) status = strings["online_connecting"] ?: "Disconnected"
                }
            }
        }
    }

    // Cleanup on exit
    DisposableEffect(Unit) {
        onDispose { wsManager.disconnect() }
    }

    if (!gameStarted) {
        // ── Lobby UI ──
        Box(modifier = Modifier.fillMaxSize().background(BgTealDark), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp).verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    TextButton(onClick = { wsManager.disconnect(); onBack() }) {
                        Text("< ${strings["back"] ?: "Back"}", color = Color.White)
                    }
                }

                Icon(painterResource(R.drawable.ic_online), null, tint = Color.White, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("ONLINE PLAY", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))

                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(strings["username_label"] ?: "Player Name", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = BlueTop, unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Room Code
                OutlinedTextField(
                    value = roomCode,
                    onValueChange = { roomCode = it.uppercase() },
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
                    val code = if (roomCode.isBlank()) {
                        val gen = "ROOM-" + (100000..999999).random()
                        roomCode = gen
                        gen
                    } else roomCode
                    status = strings["creating_room"] ?: "Creating room..."
                    wsManager.joinRoom(code, username)
                })
                Spacer(modifier = Modifier.height(12.dp))
                GlossyButton("JOIN ROOM", R.drawable.ic_two_players, isGreen = false, onClick = {
                    if (roomCode.isNotBlank()) {
                        status = strings["joining_room"] ?: "Joining room..."
                        wsManager.joinRoom(roomCode, username)
                    }
                })
                Spacer(modifier = Modifier.height(12.dp))
                GlossyButton(strings["quick_match"] ?: "QUICK MATCH", R.drawable.ic_online, isGreen = false, onClick = {
                    status = strings["online_waiting"] ?: "Waiting..."
                    wsManager.quickMatch(username)
                })

                if (status.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(status, color = Color.LightGray, fontSize = 14.sp, textAlign = TextAlign.Center)
                }

                if (roomCode.isNotBlank() && status.contains("Waiting", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📋 Room: $roomCode", color = GoldTrim, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // ── Online Game UI ──
        OnlineGameScreen(
            engine = engine,
            wsManager = wsManager,
            strings = strings,
            myColor = myColor,
            opponentName = opponentName,
            chatMessages = chatMessages,
            chatInput = chatInput,
            onChatInputChange = { chatInput = it },
            onSendChat = {
                if (chatInput.isNotBlank()) {
                    wsManager.sendChat(chatInput)
                    chatMessages = chatMessages + "You: $chatInput"
                    chatInput = ""
                }
            },
            drawOffered = drawOffered,
            onAcceptDraw = { wsManager.acceptDraw(); drawOffered = false },
            onDeclineDraw = { wsManager.declineDraw(); drawOffered = false },
            gameOverMessage = gameOverMessage,
            onBack = { wsManager.disconnect(); onBack() }
        )
    }
}

// ──────────────────────────────────────────────────────────────
//  Online Game Screen
// ──────────────────────────────────────────────────────────────
@Composable
fun OnlineGameScreen(
    engine: ChessEngine,
    wsManager: WebSocketManager,
    strings: Map<String, String>,
    myColor: String,
    opponentName: String,
    chatMessages: List<String>,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    onSendChat: () -> Unit,
    drawOffered: Boolean,
    onAcceptDraw: () -> Unit,
    onDeclineDraw: () -> Unit,
    gameOverMessage: String?,
    onBack: () -> Unit
) {
    val unicodePieces = mapOf(
        "wK" to "♔", "wQ" to "♕", "wR" to "♖", "wB" to "♗", "wN" to "♘", "wP" to "♙",
        "bK" to "♚", "bQ" to "♛", "bR" to "♜", "bB" to "♝", "bN" to "♞", "bP" to "♟"
    )

    val myEngineColor = if (myColor == "white") 'w' else 'b'
    val isMyTurn = engine.turn == myEngineColor

    var selectedSquare by remember { mutableStateOf<com.karlitodev.meshchess.engine.Pos?>(null) }
    var legalMoves by remember { mutableStateOf(listOf<com.karlitodev.meshchess.engine.ChessMove>()) }
    // Force recomposition on FEN change
    var boardState by remember { mutableStateOf(0) }

    // Flip board if playing black
    val viewAsBlack = myColor == "black"

    Column(
        modifier = Modifier.fillMaxSize().background(BgTealDark),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("< ${strings["quit"] ?: "Quit"}", color = Color.White) }
            Text("vs $opponentName", color = GoldTrim, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterVertically))
        }

        // Color & Turn indicator
        val colorName = if (myColor == "white") (strings["whites"] ?: "White") else (strings["blacks"] ?: "Black")
        Text(
            "${strings["you_are"] ?: "You are"} $colorName",
            color = Color.LightGray, fontSize = 13.sp
        )
        val turnText = if (isMyTurn) (strings["your_turn"] ?: "Your turn") else (strings["opponent_turn"] ?: "Opponent's turn")
        Text(turnText, color = if (isMyTurn) GreenTop else BlueTop, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(8.dp))

        // Board
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .aspectRatio(1f)
                .border(4.dp, WoodLight)
                .background(WoodDark)
        ) {
            val squareSize = maxWidth / 8
            Column(modifier = Modifier.fillMaxSize()) {
                val ranks = if (viewAsBlack) (0..7) else (7 downTo 0)
                val files = if (viewAsBlack) (7 downTo 0) else (0..7)
                for (r in ranks) {
                    Row(modifier = Modifier.fillMaxWidth().height(squareSize)) {
                        for (f in files) {
                            val isDark = (r + f) % 2 == 0
                            val bgColor = if (isDark) Color(0xFF769656) else Color(0xFFEEEED2)
                            val isSelected = selectedSquare?.file == f && selectedSquare?.rank == r
                            val isMoveTarget = legalMoves.any { it.to.file == f && it.to.rank == r }
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
                                    .clickable {
                                        if (!isMyTurn || gameOverMessage != null) return@clickable

                                        // If clicking on a legal move target, make the move
                                        if (selectedSquare != null) {
                                            val matchingMoves = legalMoves.filter { it.to.file == f && it.to.rank == r }
                                            if (matchingMoves.isNotEmpty()) {
                                                val move = matchingMoves.first()
                                                val fromSq = engine.coordToSq(move.from.file, move.from.rank)
                                                val toSq = engine.coordToSq(move.to.file, move.to.rank)
                                                val promo = move.promotion?.toString()
                                                engine.applyMove(move)
                                                wsManager.sendMove(fromSq, toSq, promo)
                                                selectedSquare = null
                                                legalMoves = emptyList()
                                                boardState++
                                                return@clickable
                                            }
                                        }

                                        // Select own piece
                                        val piece = engine.pieceAt(f, r)
                                        if (piece != null && piece[0] == myEngineColor) {
                                            selectedSquare = com.karlitodev.meshchess.engine.Pos(f, r)
                                            legalMoves = engine.legalMovesFrom(f, r)
                                        } else {
                                            selectedSquare = null
                                            legalMoves = emptyList()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val piece = engine.board[r][f]
                                if (piece != null) {
                                    val isWhitePiece = piece[0] == 'w'
                                    if (isWhitePiece) {
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
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = unicodePieces[piece] ?: "",
                                            fontSize = (squareSize.value * 0.68f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111111)
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

        Spacer(modifier = Modifier.height(8.dp))

        // Game over
        if (gameOverMessage != null) {
            Text(gameOverMessage, color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Draw offer
        if (drawOffered) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(strings["draw_offered"] ?: "Draw offered", color = GoldTrim, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlossyButton(strings["accept_draw"] ?: "Accept", R.drawable.ic_two_players, isGreen = true, onClick = onAcceptDraw)
                GlossyButton(strings["decline_draw"] ?: "Decline", R.drawable.ic_two_players, isGreen = false, onClick = onDeclineDraw)
            }
        }

        // Action buttons
        if (gameOverMessage == null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallButton(strings["resign"] ?: "Resign", Color.Red) { wsManager.resign() }
                SmallButton(strings["offer_draw"] ?: "Draw", GoldTrim) { wsManager.offerDraw() }
            }
        }

        // Chat (compact)
        Spacer(modifier = Modifier.height(8.dp))
        if (chatMessages.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = 80.dp)) {
                chatMessages.takeLast(3).forEach { msg ->
                    Text(msg, color = Color.LightGray, fontSize = 11.sp)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = onChatInputChange,
                placeholder = { Text(strings["chat_hint"] ?: "Message...", color = Color.Gray, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = BlueTop, unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier.weight(1f).height(48.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            SmallButton(strings["send"] ?: "Send", BlueTop, onSendChat)
        }
    }
}

@Composable
fun SmallButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.8f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ──────────────────────────────────────────────────────────────
//  Puzzle List Screen
// ──────────────────────────────────────────────────────────────
@Composable
fun PuzzleListScreen(
    strings: Map<String, String>,
    lang: String,
    onPuzzleSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    val puzzles = PuzzleLibrary.puzzles
    val beginner = puzzles.filter { it.difficulty == "beginner" }
    val intermediate = puzzles.filter { it.difficulty == "intermediate" }
    val advanced = puzzles.filter { it.difficulty == "advanced" }

    Column(
        modifier = Modifier.fillMaxSize().background(BgTealDark).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Text("< ${strings["back"] ?: "Back"}", color = Color.White) }
        }
        Text(
            "♟ ${strings["puzzles_title"] ?: "Puzzles"}",
            color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Beginner Section
            item {
                PuzzleSectionHeader(strings["puzzles_beginner"] ?: "Beginner", GreenTop)
            }
            items(beginner) { puzzle ->
                PuzzleCard(puzzle, lang, onClick = { onPuzzleSelected(puzzle.id) })
            }

            // Intermediate Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                PuzzleSectionHeader(strings["puzzles_intermediate"] ?: "Intermediate", BlueTop)
            }
            items(intermediate) { puzzle ->
                PuzzleCard(puzzle, lang, onClick = { onPuzzleSelected(puzzle.id) })
            }

            // Advanced Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                PuzzleSectionHeader(strings["puzzles_advanced"] ?: "Advanced", Color(0xFFFF6B35))
            }
            items(advanced) { puzzle ->
                PuzzleCard(puzzle, lang, onClick = { onPuzzleSelected(puzzle.id) })
            }
        }
    }
}

@Composable
fun PuzzleSectionHeader(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun PuzzleCard(puzzle: Puzzle, lang: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A3A)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "#${puzzle.id} — ${puzzle.title}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    puzzle.description[lang] ?: puzzle.description["en"] ?: "",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
            Text("▶", color = GoldTrim, fontSize = 20.sp)
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  Puzzle Game Screen
// ──────────────────────────────────────────────────────────────
@Composable
fun PuzzleGameScreen(
    puzzleId: Int,
    strings: Map<String, String>,
    lang: String,
    onBack: () -> Unit
) {
    val puzzle = PuzzleLibrary.getById(puzzleId) ?: return

    val engine = remember { ChessEngine().apply { loadFen(puzzle.fen) } }
    var solutionIndex by remember { mutableStateOf(0) }
    var feedbackMessage by remember { mutableStateOf("") }
    var isSolved by remember { mutableStateOf(false) }
    var selectedSquare by remember { mutableStateOf<com.karlitodev.meshchess.engine.Pos?>(null) }
    var legalMoves by remember { mutableStateOf(listOf<com.karlitodev.meshchess.engine.ChessMove>()) }
    var boardVersion by remember { mutableStateOf(0) }

    val unicodePieces = mapOf(
        "wK" to "♔", "wQ" to "♕", "wR" to "♖", "wB" to "♗", "wN" to "♘", "wP" to "♙",
        "bK" to "♚", "bQ" to "♛", "bR" to "♜", "bB" to "♝", "bN" to "♞", "bP" to "♟"
    )

    Column(
        modifier = Modifier.fillMaxSize().background(BgTealDark),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Text("< ${strings["back"] ?: "Back"}", color = Color.White) }
        }

        Text(
            "#${puzzle.id} — ${puzzle.title}",
            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            puzzle.description[lang] ?: puzzle.description["en"] ?: "",
            color = Color.LightGray, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Progress
        Text(
            "${solutionIndex} ${strings["puzzle_of"] ?: "of"} ${puzzle.solution.size}",
            color = GoldTrim, fontSize = 14.sp, fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Board
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .aspectRatio(1f)
                .border(4.dp, WoodLight)
                .background(WoodDark)
        ) {
            val squareSize = maxWidth / 8
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 7 downTo 0) {
                    Row(modifier = Modifier.fillMaxWidth().height(squareSize)) {
                        for (f in 0..7) {
                            val isDark = (r + f) % 2 == 0
                            val bgColor = if (isDark) Color(0xFF769656) else Color(0xFFEEEED2)
                            val isSelected = selectedSquare?.file == f && selectedSquare?.rank == r
                            val isMoveTarget = legalMoves.any { it.to.file == f && it.to.rank == r }
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
                                    .clickable {
                                        if (isSolved) return@clickable

                                        if (selectedSquare != null) {
                                            val matchingMoves = legalMoves.filter { it.to.file == f && it.to.rank == r }
                                            if (matchingMoves.isNotEmpty()) {
                                                val move = matchingMoves.first()
                                                val moveStr = engine.coordToSq(move.from.file, move.from.rank) +
                                                        engine.coordToSq(move.to.file, move.to.rank) +
                                                        (move.promotion?.lowercaseChar() ?: "")

                                                val expectedMove = puzzle.solution[solutionIndex].lowercase()

                                                if (moveStr.lowercase() == expectedMove) {
                                                    engine.applyMove(move)
                                                    solutionIndex++
                                                    boardVersion++

                                                    if (solutionIndex >= puzzle.solution.size) {
                                                        feedbackMessage = strings["puzzle_complete"] ?: "Puzzle solved! 🎉"
                                                        isSolved = true
                                                    } else {
                                                        feedbackMessage = strings["puzzle_correct"] ?: "Correct! ✅"
                                                        // Auto-play opponent's response if there's one
                                                        if (solutionIndex < puzzle.solution.size) {
                                                            val opponentMoveStr = puzzle.solution[solutionIndex]
                                                            val fromSq = opponentMoveStr.substring(0, 2)
                                                            val toSq = opponentMoveStr.substring(2, 4)
                                                            val promo = if (opponentMoveStr.length > 4) opponentMoveStr[4].uppercaseChar() else null
                                                            val opponentMove = engine.findLegalMove(fromSq, toSq, promo)
                                                            if (opponentMove != null) {
                                                                engine.applyMove(opponentMove)
                                                                solutionIndex++
                                                                boardVersion++
                                                            }
                                                            if (solutionIndex >= puzzle.solution.size) {
                                                                feedbackMessage = strings["puzzle_complete"] ?: "Puzzle solved! 🎉"
                                                                isSolved = true
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    feedbackMessage = strings["puzzle_wrong"] ?: "Incorrect, try again ❌"
                                                }

                                                selectedSquare = null
                                                legalMoves = emptyList()
                                                return@clickable
                                            }
                                        }

                                        // Select piece
                                        val piece = engine.pieceAt(f, r)
                                        if (piece != null && piece[0] == engine.turn) {
                                            selectedSquare = com.karlitodev.meshchess.engine.Pos(f, r)
                                            legalMoves = engine.legalMovesFrom(f, r)
                                        } else {
                                            selectedSquare = null
                                            legalMoves = emptyList()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                @Suppress("UNUSED_EXPRESSION")
                                boardVersion // trigger recomposition
                                val piece = engine.board[r][f]
                                if (piece != null) {
                                    val isWhitePiece = piece[0] == 'w'
                                    if (isWhitePiece) {
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
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = unicodePieces[piece] ?: "",
                                            fontSize = (squareSize.value * 0.68f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111111)
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

        // Feedback
        if (feedbackMessage.isNotBlank()) {
            Text(
                feedbackMessage,
                color = if (isSolved) GreenTop else if (feedbackMessage.contains("❌")) Color.Red else GoldTrim,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isSolved) {
            Spacer(modifier = Modifier.height(12.dp))
            GlossyButton(strings["back"] ?: "Back to Puzzles", R.drawable.ic_puzzles, isGreen = true, onClick = onBack)
        }
    }
}

// ──────────────────────────────────────────────────────────────
//  Game Screen (Local & AI — NO board rotation)
// ──────────────────────────────────────────────────────────────
@Composable
fun GameScreen(viewModel: GameViewModel, strings: Map<String, String>, isLocalMultiplayer: Boolean, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val unicodePieces = mapOf(
        "wK" to "♔", "wQ" to "♕", "wR" to "♖", "wB" to "♗", "wN" to "♘", "wP" to "♙",
        "bK" to "♚", "bQ" to "♛", "bR" to "♜", "bB" to "♝", "bN" to "♞", "bP" to "♟"
    )

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

        // Large Responsive Chessboard — NO rotation in any mode
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .aspectRatio(1f)
                .border(4.dp, WoodLight)
                .background(WoodDark)
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
                                        // High contrast backdrop pill/badge for white pieces
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
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = unicodePieces[piece] ?: "",
                                            fontSize = (squareSize.value * 0.68f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111111)
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
