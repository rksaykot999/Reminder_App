package com.example.reminderapp

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.reminderapp.auth.AuthViewModel
import com.example.reminderapp.auth.LoginScreen
import com.example.reminderapp.auth.SignupScreen
import com.example.reminderapp.data.AppDatabase
import com.example.reminderapp.pomodoro.FocusTimerScreen
import com.example.reminderapp.reminders.HomeScreen
import com.example.reminderapp.settings.ProfileScreen
import com.example.reminderapp.settings.SettingScreen
import com.example.reminderapp.settings.SettingsViewModel
import com.example.reminderapp.stopwatch.StopwatchAndTimerScreen
import com.example.reminderapp.ui.screens.CalendarScreen
import com.example.reminderapp.ui.theme.AppTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val denied = permissions.filter { !it.value }.keys
        if (denied.isNotEmpty()) {
            Toast.makeText(this, "Denied: ${denied.joinToString()}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseApp.initializeApp(this)
        checkAndRequestPermissions()

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()

            AppTheme(darkTheme = isDarkMode) {
                MainRoot(settingsViewModel)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "All", Icons.Default.CheckCircle)
    object Calendar : Screen("calendar", "Calendar", Icons.Default.DateRange)
    object Pomodoro : Screen("pomodoro", "Pomodoro", Icons.Default.Timelapse)
    object Stopwatch : Screen("stopwatch", "Stopwatch", Icons.Default.Timer)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Signup : Screen("signup", "Signup", Icons.Default.PersonAdd)
}

@Composable
fun MainRoot(settingsViewModel: SettingsViewModel) {
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.user.collectAsState()
    val isGuest by authViewModel.isGuest.collectAsState()
    val navController = rememberNavController()
    
    val lockType by settingsViewModel.lockType.collectAsState()
    var isUnlocked by remember { mutableStateOf(lockType == "NONE") }

    if (currentUser == null && !isGuest) {
        NavHost(navController, startDestination = Screen.Login.route) {
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToSignUp = { navController.navigate(Screen.Signup.route) },
                    onLoginSuccess = { /* Navigated by currentUser state change */ }
                )
            }
            composable(Screen.Signup.route) {
                SignupScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = { navController.popBackStack() },
                    onSignupSuccess = { /* Navigated by currentUser state change */ }
                )
            }
        }
    } else {
        val context = LocalContext.current
        if (!isUnlocked && lockType != "NONE") {
            LockScreen(
                lockType = lockType,
                expectedValue = settingsViewModel.getLockValue(),
                onUnlock = { isUnlocked = true }
            )
        } else {
            MainScreen(
                isDarkMode = settingsViewModel.isDarkMode.collectAsState().value,
                onDarkModeChange = settingsViewModel::setDarkMode,
                onLogout = {
                    authViewModel.signOut(context) {
                        isUnlocked = lockType == "NONE"
                    }
                }
            )
        }
    }
}

@Composable
fun LockScreen(lockType: String, expectedValue: String, onUnlock: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "App Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                "Please enter your $lockType to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedTextField(
                value = input,
                onValueChange = { 
                    input = it
                    error = false
                },
                label = { Text("Enter $lockType") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = if (lockType == "PIN") KeyboardType.Number else KeyboardType.Password),
                shape = RoundedCornerShape(16.dp),
                isError = error,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )
            
            AnimatedVisibility(visible = error) {
                Text(
                    "Incorrect $lockType. Please try again.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (input == expectedValue || expectedValue.isEmpty()) {
                        onUnlock()
                    } else {
                        error = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                )
            ) {
                Text("UNLOCK", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isDarkMode: Boolean, 
    onDarkModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val timerViewModel: TimerViewModel = viewModel(
        factory = TimerViewModel.Factory(database.eventDao(), database.historyDao())
    )
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Calendar, Screen.Pomodoro, Screen.Stopwatch, Screen.Settings)

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                tonalElevation = 8.dp,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        
                        CustomNavItem(
                            screen = screen,
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(timerViewModel)
            }
            composable(Screen.Pomodoro.route) {
                FocusTimerScreen(isDarkMode = isDarkMode)
            }
            composable(Screen.Stopwatch.route) {
                StopwatchAndTimerScreen(timerViewModel)
            }
            composable(Screen.Settings.route) {
                SettingScreen(
                    isDarkMode = isDarkMode, 
                    onDarkModeChange = onDarkModeChange,
                    onLogout = onLogout,
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun CustomNavItem(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.4f),
        label = "color"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent,
        label = "containerColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = MaterialTheme.colorScheme.onSurface),
                onClick = onClick
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = screen.label,
            tint = contentColor,
            modifier = Modifier
                .size(26.dp)
                .scale(if (selected) 1.2f else 1f)
        )
    }
}
