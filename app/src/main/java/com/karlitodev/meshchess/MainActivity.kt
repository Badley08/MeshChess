package com.karlitodev.meshchess

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.karlitodev.meshchess.language.en.EnglishUI
import com.karlitodev.meshchess.language.es.EspagnolUI
import com.karlitodev.meshchess.language.fr.FrenchUI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var selectedLang by remember { mutableStateOf("fr") }
            val strings = when (selectedLang) {
                "en" -> EnglishUI.strings
                "es" -> EspagnolUI.strings
                else -> FrenchUI.strings
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = strings["app_name"] ?: "MeshChess",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { /* TODO: Start local game */ },
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Text(strings["play_local"] ?: "Play Local")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { /* TODO: Start online game */ },
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            Text(strings["play_online"] ?: "Play Online")
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Language Selector
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedLang == "fr",
                                onClick = { selectedLang = "fr" },
                                label = { Text("FR") }
                            )
                            FilterChip(
                                selected = selectedLang == "en",
                                onClick = { selectedLang = "en" },
                                label = { Text("EN") }
                            )
                            FilterChip(
                                selected = selectedLang == "es",
                                onClick = { selectedLang = "es" },
                                label = { Text("ES") }
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = strings["app_description"] ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = strings["creator_bio"] ?: "",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedButton(
                            onClick = {
                                GitHubUpdater(this@MainActivity).checkForUpdates()
                            }
                        ) {
                            Text(strings["check_updates"] ?: "Check for Updates")
                        }
                    }
                }
            }
        }
    }
}
