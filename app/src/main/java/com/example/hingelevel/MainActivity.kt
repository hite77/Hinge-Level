package com.example.hingelevel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hingelevel.ui.theme.HingeLevelTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : ComponentActivity() {
    private val viewModel: LevelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HingeLevelTheme {
                LevelTrackerScreen(viewModel)
            }
        }
    }
}

@Composable
fun LevelTrackerScreen(viewModel: LevelViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var currentLevel by remember { mutableStateOf("") }
    var dayAtLevel by remember { mutableStateOf("") }
    var goalLevel by remember { mutableStateOf("") }

    // Initialize fields with the latest data if available
    LaunchedEffect(uiState.latestLevel) {
        uiState.latestLevel?.let {
            currentLevel = it.level.toString()
            dayAtLevel = it.dayAtLevel.toString()
            goalLevel = it.goalLevel?.toString() ?: ""
        }
    }


    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Level Tracker", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            // -- Current Level Section --
            Card(elevation = CardDefaults.cardElevation(4.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Last Recorded Level",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    val latest = uiState.latestLevel
                    if (latest != null) {
                        val goalText = latest.goalLevel?.let { " of $it" } ?: ""
                        Text(
                            "Level ${latest.level} (Day ${latest.dayAtLevel})$goalText",
                            style = MaterialTheme.typography.displaySmall
                        )
                    } else {
                        Text(
                            "No data yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(32.dp))

            // -- Input Section --
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentLevel,
                    onValueChange = { currentLevel = it },
                    label = { Text("Level") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = dayAtLevel,
                    onValueChange = { dayAtLevel = it },
                    label = { Text("Day at Level") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = goalLevel,
                    onValueChange = { goalLevel = it },
                    label = { Text("Goal Level") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val levelInt = currentLevel.toIntOrNull()
                    val dayInt = dayAtLevel.toIntOrNull()
                    val goalInt = goalLevel.toIntOrNull()
                    if (levelInt != null && dayInt != null) {
                        viewModel.recordLevel(levelInt, dayInt, goalInt)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Record Today's Level")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // -- History Section --
            Text("Last 7 Days", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            // LazyColumn is used here for an efficient, independently scrollable list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // This makes the list fill the remaining space
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.lastSevenDays.entries.sortedByDescending { it.key }) { (date, data) ->
                    HistoryRow(date = date, data = data)
                }
            }
        }
    }
}

@Composable
fun HistoryRow(date: LocalDate, data: LevelData?) {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (data != null) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = if (date == LocalDate.now()) "Today" else date.format(dateFormatter),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = date.format(dayOfWeekFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        if (data != null) {
            val goalText = data.goalLevel?.let { " of $it" } ?: ""
            Text(
                text = "Lvl: ${data.level}, Day: ${data.dayAtLevel}$goalText",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                fontSize = 18.sp
            )
        } else {
            Text(
                text = "No Data",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.End
            )
        }
    }
}
