package com.example.reminderapp.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.media.RingtoneManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reminderapp.TimerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(viewModel: TimerViewModel) {
    val remainingTime by viewModel.remainingTime.collectAsState()
    val initialTime by viewModel.initialTime.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val history by viewModel.history.collectAsState()
    val context = LocalContext.current
    
    var showHistory by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            
            // Logic to parse "1 minute", "5 minutes", etc.
            val regex = Regex("(\\d+)\\s*(minute|minutes|min)")
            val match = regex.find(spokenText.lowercase())
            if (match != null) {
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                viewModel.setTimer(minutes * 60 * 1000L)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.timerFinishedEvent.collectLatest {
            try {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(context, notification)
                r.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with History
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { showHistory = true },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.History, contentDescription = "History", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Circular Progress Countdown with Integrated Voice Control
        TimerDisplay(
            progress = if (initialTime > 0) remainingTime.toFloat() / initialTime else 0f,
            timeString = viewModel.formatTime(remainingTime),
            isRunning = isRunning,
            onVoiceClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a duration (e.g., 5 minutes)")
                }
                speechLauncher.launch(intent)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tiered Quick Start Presets
        QuickStartTiers(
            presets = viewModel.quickPresets,
            onSelect = { viewModel.setTimer(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Intuitive Manual Time Input Section
        IntuitiveManualTimer(viewModel = viewModel)

        Spacer(modifier = Modifier.weight(1f))

        // Main Control Cluster (Restart, Play/Pause)
        TimerControlCluster(
            isRunning = isRunning,
            onToggle = { if (isRunning) viewModel.pauseTimer() else viewModel.startTimer() },
            onReset = { viewModel.resetTimer() }
        )
    }

    if (showHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TIMER HISTORY",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { showHistory = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (history.isEmpty()) {
                    Text(
                        "No history logs yet.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(history) { log ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = log.message,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = log.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TimerDisplay(progress: Float, timeString: String, isRunning: Boolean, onVoiceClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Pulse effect when running
    val scale by animateFloatAsState(
        targetValue = if (isRunning) 1.02f else 1.0f,
        animationSpec = if (isRunning) {
            infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(500)
        },
        label = "pulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(280.dp)
            .scale(scale)
    ) {
        // Particle Glow & Ring
        val primaryColor = MaterialTheme.colorScheme.primary
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val radius = size.minDimension / 2 - strokeWidth

            // Background Ring
            drawCircle(
                color = Color.DarkGray.copy(alpha = 0.2f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // Glowing Ring
            drawArc(
                brush = Brush.sweepGradient(
                    0f to primaryColor.copy(alpha = 0.1f),
                    0.5f to primaryColor,
                    1f to primaryColor.copy(alpha = 0.1f)
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Subtle Particle Glow around the ring
            val angle = (-90f + 360f * progress).toDouble()
            val dotX = (size.width / 2) + radius * cos(Math.toRadians(angle)).toFloat()
            val dotY = (size.height / 2) + radius * sin(Math.toRadians(angle)).toFloat()

            drawCircle(
                color = primaryColor,
                radius = 8.dp.toPx(),
                center = Offset(dotX, dotY),
                alpha = glowAlpha
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.HourglassBottom,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = timeString,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            VoiceButton(onListenClick = onVoiceClick)
        }
    }
}

@Composable
fun VoiceButton(onListenClick: () -> Unit) {
    var isListening by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )

    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(rippleScale)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
            )
        }
        
        FilledIconButton(
            onClick = { 
                isListening = true
                onListenClick()
            },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.primary
            ),
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice"
            )
        }
    }
    
    // Auto reset listening animation
    LaunchedEffect(isListening) {
        if (isListening) {
            delay(3000)
            isListening = false
        }
    }
}

@Composable
fun TimerControlCluster(
    isRunning: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Restart Button
        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = "Restart", tint = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Main Play/Pause
        LargePlayButton(isRunning = isRunning, onClick = onToggle)
    }
}

@Composable
fun QuickStartTiers(presets: List<Pair<String, Long>>, onSelect: (Long) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "QUICK START TIERS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presets.forEach { (label, millis) ->
                val minutes = label.split(" ")[0]
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clickable { onSelect(millis) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = minutes,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "MIN",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LargePlayButton(isRunning: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        val transition = updateTransition(targetState = isRunning, label = "playPause")
        val scale by transition.animateFloat(label = "scale") { state ->
            if (state) 1.1f else 1.0f
        }

        FilledIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(84.dp)
                .scale(scale)
                .shadow(16.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Toggle",
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
fun IntuitiveManualTimer(viewModel: TimerViewModel) {
    var hours by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }
    
    val isRunning by viewModel.isRunning.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "MANUAL SETUP",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            letterSpacing = 2.sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DigitalTimeField(value = hours, onValueChange = { if (it.length <= 2) hours = it }, label = "HH")
            Text(":", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            DigitalTimeField(value = minutes, onValueChange = { if (it.length <= 2 && (it.toIntOrNull() ?: 0) <= 59) minutes = it }, label = "MM")
            Text(":", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            DigitalTimeField(value = seconds, onValueChange = { if (it.length <= 2 && (it.toIntOrNull() ?: 0) <= 59) seconds = it }, label = "SS")
        }

        Button(
            onClick = {
                val h = hours.toLongOrNull() ?: 0L
                val m = minutes.toLongOrNull() ?: 0L
                val s = seconds.toLongOrNull() ?: 0L
                val totalMillis = (h * 3600 + m * 60 + s) * 1000
                if (totalMillis > 0) {
                    viewModel.setTimer(totalMillis)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            ),
            enabled = !isRunning
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("START CUSTOM TIMER", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun DigitalTimeField(value: String, onValueChange: (String) -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.all { c -> c.isDigit() }) onValueChange(it) },
            modifier = Modifier.width(48.dp),
            textStyle = MaterialTheme.typography.titleSmall.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            placeholder = {
                Text(
                    "00",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    fontFamily = FontFamily.Monospace
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(8.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTimePickerDialog(onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = 0, initialMinute = 0, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("SET")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Select Duration",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                TimeInput(state = state)
            }
        }
    )
}
