package com.karlitodev.meshchess.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karlitodev.meshchess.BgTealDark
import com.karlitodev.meshchess.BgTealLight
import com.karlitodev.meshchess.GlossyButton
import com.karlitodev.meshchess.R
import com.karlitodev.meshchess.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    settingsManager: SettingsManager,
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    
    var selectedLang by remember { mutableStateOf(settingsManager.getLanguage()) }
    var apiKey by remember { mutableStateOf(settingsManager.getGeminiApiKey() ?: "") }

    val bgBrush = Brush.radialGradient(
        colors = listOf(BgTealLight, BgTealDark),
        radius = 1500f
    )

    Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomePage(
                    onNext = { coroutineScope.launch { pagerState.animateScrollToPage(1) } }
                )
                1 -> LanguagePage(
                    selectedLang = selectedLang,
                    onLangSelected = { 
                        selectedLang = it
                        settingsManager.saveLanguage(it)
                    },
                    onNext = { coroutineScope.launch { pagerState.animateScrollToPage(2) } }
                )
                2 -> ApiKeyPage(
                    apiKey = apiKey,
                    onKeyChanged = { apiKey = it },
                    onComplete = {
                        settingsManager.saveGeminiApiKey(apiKey)
                        settingsManager.setFirstLaunchCompleted()
                        onComplete()
                    }
                )
            }
        }
        
        // Page Indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (isSelected) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color.Gray)
                )
            }
        }
    }
}

@Composable
fun WelcomePage(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.mesh_chess),
            contentDescription = "Logo",
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Welcome to MeshChess",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Experience the ultimate chess game with beautiful graphics and AI opponents.",
            color = Color.LightGray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        GlossyButton(text = "GET STARTED", iconRes = R.drawable.ic_rocket, isGreen = true, onClick = onNext)
    }
}

@Composable
fun LanguagePage(
    selectedLang: String,
    onLangSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Choose Your Language",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        LanguageOption("en", "English", selectedLang == "en") { onLangSelected("en") }
        Spacer(modifier = Modifier.height(16.dp))
        LanguageOption("fr", "Français", selectedLang == "fr") { onLangSelected("fr") }
        Spacer(modifier = Modifier.height(16.dp))
        LanguageOption("es", "Español", selectedLang == "es") { onLangSelected("es") }
        
        Spacer(modifier = Modifier.height(48.dp))
        GlossyButton(text = "CONTINUE", iconRes = R.drawable.ic_arrow_forward, isGreen = true, onClick = onNext)
    }
}

@Composable
fun LanguageOption(code: String, name: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E8A08) else Color(0xFF086A8A)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = code.uppercase(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ApiKeyPage(
    apiKey: String,
    onKeyChanged: (String) -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Play vs AI",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "To play against the computer, you need a free Gemini API key.",
            color = Color.LightGray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = apiKey,
            onValueChange = onKeyChanged,
            label = { Text("Gemini API Key", color = Color.White) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4AC4E7),
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color(0xFF4AC4E7)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "You can get one at aistudio.google.com/app/apikey. Leave blank to skip for now.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        GlossyButton(text = "FINISH SETUP", iconRes = R.drawable.ic_celebrate, isGreen = true, onClick = onComplete)
    }
}
