package com.example.reminderapp.pomodoro

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar
import java.util.Locale

@Composable
fun FocusTimerScreen(viewModel: FocusTimerViewModel = viewModel(), isDarkMode: Boolean = true) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val backgroundColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFF0F1014) else Color(0xFFF5F5F5),
        label = "BackgroundColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isDarkMode) Color.White else Color.Black,
        label = "TextColor"
    )

    LaunchedEffect(Unit) {
        createNotificationChannel(context)
        viewModel.setOnTimerFinishedListener {
            showNotification(context)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Focus Timer",
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 40.dp)
                )

                Spacer(modifier = Modifier.weight(0.3f))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(260.dp)
                ) {
                    TimerProgressCircle(
                        progress = uiState.timeLeftSeconds.toFloat() / uiState.totalSeconds.toFloat(),
                        state = uiState.timerState,
                        isDarkMode = isDarkMode
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BreathingIndicator(isRunning = uiState.isRunning)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatTime(uiState.timeLeftSeconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 60.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )
                        Text(
                            text = uiState.timerState.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = getThemeColor(uiState.timerState).copy(alpha = 0.7f),
                            letterSpacing = 4.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.2f))

                WeeklyAnalyticsSection(data = uiState.weeklyFocusedMinutes, isDarkMode = isDarkMode)

                Spacer(modifier = Modifier.weight(0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InfoColumn(label = "Round", value = "${uiState.currentRound}/4", isDarkMode = isDarkMode)
                    InfoColumn(label = "Today", value = "${uiState.totalSessionsCompleted}", isDarkMode = isDarkMode)
                }

                Spacer(modifier = Modifier.weight(0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.resetTimer() }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Default.Refresh, "Reset", tint = textColor.copy(0.4f))
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    LargeFloatingActionButton(
                        onClick = { viewModel.toggleTimer() },
                        containerColor = getThemeColor(uiState.timerState)
                    ) {
                        if (uiState.isRunning) {
                            Canvas(modifier = Modifier.size(20.dp)) {
                                drawRect(Color.Black, size = size.copy(width = size.width * 0.3f))
                                drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.7f, 0f), size = size.copy(width = size.width * 0.3f))
                            }
                        } else {
                            Icon(Icons.Default.PlayArrow, "Start", modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(88.dp))
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun WeeklyAnalyticsSection(data: List<Int>, isDarkMode: Boolean) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val maxMinutes = (data.maxOrNull() ?: 1).coerceAtLeast(60)
    
    val calendar = Calendar.getInstance()
    val rawDay = calendar.get(Calendar.DAY_OF_WEEK)
    val todayIndex = if (rawDay == Calendar.SUNDAY) 6 else rawDay - 2

    val cardBg = if (isDarkMode) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.04f)
    val textColor = if (isDarkMode) Color.White else Color.Black

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WEEKLY ACTIVITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Cyan.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${data.sum()}m total",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.4f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { index, value ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (value > 0) {
                            Text(
                                text = "${value}m",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (index == todayIndex) Color.Cyan else textColor.copy(0.4f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        
                        val barHeight = (value.toFloat() / maxMinutes.toFloat()).coerceIn(0.05f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(barHeight)
                                .width(10.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (index == todayIndex) Color.Cyan else textColor.copy(0.15f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = days[index],
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (index == todayIndex) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (index == todayIndex) Color.Cyan else textColor.copy(0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimerProgressCircle(progress: Float, state: TimerState, isDarkMode: Boolean) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "Progress")
    val bgColor = if (isDarkMode) Color.White.copy(0.05f) else Color.Black.copy(0.05f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(bgColor, style = Stroke(width = 10.dp.toPx()))
        drawArc(
            color = getThemeColor(state),
            startAngle = -90f,
            sweepAngle = 360 * animatedProgress,
            useCenter = false,
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun BreathingIndicator(isRunning: Boolean) {
    val transition = rememberInfiniteTransition(label = "Breathe")
    val scale by transition.animateFloat(1f, if (isRunning) 1.6f else 1f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "S")
    val alpha by transition.animateFloat(0.3f, if (isRunning) 1f else 0.3f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "A")
    Box(Modifier.size(10.dp).graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha).drawBehind { drawCircle(Color.Cyan) })
}

@Composable
fun InfoColumn(label: String, value: String, isDarkMode: Boolean) {
    val textColor = if (isDarkMode) Color.White else Color.Black
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = textColor.copy(0.3f))
        Text(value, style = MaterialTheme.typography.titleLarge, color = textColor, fontWeight = FontWeight.Bold)
    }
}

fun getThemeColor(state: TimerState) = when (state) {
    TimerState.FOCUS -> Color(0xFFFF5252)
    TimerState.SHORT_BREAK -> Color(0xFF64FFDA)
    TimerState.LONG_BREAK -> Color(0xFF448AFF)
    else -> Color.White
}

private fun formatTime(s: Long) = String.format(Locale.getDefault(), "%02d:%02d", s / 60, s % 60)

@Composable
fun LargeFloatingActionButton(onClick: () -> Unit, containerColor: Color, content: @Composable () -> Unit) {
    FloatingActionButton(onClick, Modifier.size(72.dp), CircleShape, containerColor, Color.Black, elevation = FloatingActionButtonDefaults.elevation(0.dp), content = content)
}

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel("TIMER_CHANNEL", "Timer", NotificationManager.IMPORTANCE_HIGH)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}

private fun showNotification(context: Context) {
    context.sendBroadcast(Intent(context, PomodoroAlarmReceiver::class.java))
    val stopIntent = Intent(context, PomodoroAlarmReceiver::class.java).apply { action = "STOP_ALARM" }
    val stopPendingIntent = PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val builder = NotificationCompat.Builder(context, "TIMER_CHANNEL")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Timer Finished!")
        .setContentText("Session complete.")
        .addAction(android.R.drawable.ic_lock_power_off, "STOP ALARM", stopPendingIntent)
        .setAutoCancel(true)
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(1, builder.build())
}
