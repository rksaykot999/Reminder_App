package com.example.reminderapp.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.reminderapp.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val lockType by settingsViewModel.lockType.collectAsState()
    val user by authViewModel.user.collectAsState()
    
    // State Control
    var isNotificationEnabled by remember { mutableStateOf(true) }
    var textScale by remember { mutableFloatStateOf(1.0f) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showLockDialog by remember { mutableStateOf(false) }
    var showSetLockValueDialog by remember { mutableStateOf(false) }
    var pendingLockType by remember { mutableStateOf("") }
    
    var selectedLanguage by remember { mutableStateOf("English") }
    var searchQuery by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Black,
                        fontSize = (28 * textScale).sp
                    )
                },
                actions = {
                    IconButton(onClick = { Toast.makeText(context, "Help center is coming soon!", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search settings...", fontSize = (14 * textScale).sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                singleLine = true
            )

            // 1. Profile Section
            ProfileSection(
                name = user?.displayName ?: "User",
                email = user?.email ?: "No email linked",
                photoUrl = user?.photoUrl?.toString(),
                textScale = textScale,
                onEditClick = { Toast.makeText(context, "Profile editing is coming soon!", Toast.LENGTH_SHORT).show() }
            )

            // 2. Personalization
            SettingGroup(title = "Personalization", textScale = textScale) {
                SettingClickableRow(
                    icon = Icons.Default.Language,
                    title = "App Language",
                    subtitle = "Currently: $selectedLanguage",
                    textScale = textScale,
                    onClick = { 
                        selectedLanguage = if (selectedLanguage == "English") "Bengali" else "English" 
                        Toast.makeText(context, "Language changed to $selectedLanguage", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingSwitchRow(
                    icon = Icons.Default.Palette,
                    title = "Dark Mode",
                    subtitle = "Applies across the whole app",
                    checked = isDarkMode,
                    textScale = textScale,
                    onCheckedChange = onDarkModeChange
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                // Text Size Slider
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Adjust Font Size", fontWeight = FontWeight.SemiBold, fontSize = (15 * textScale).sp)
                    }
                    Slider(
                        value = textScale,
                        onValueChange = { textScale = it },
                        valueRange = 0.8f..1.4f,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 3. Privacy & Security
            SettingGroup(title = "Privacy & Security", textScale = textScale) {
                SettingClickableRow(
                    icon = Icons.Default.Lock,
                    title = "Lock System",
                    subtitle = "Current: ${if (lockType == "NONE") "Disabled" else lockType}",
                    textScale = textScale,
                    onClick = { showLockDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingClickableRow(
                    icon = Icons.Default.GppGood,
                    title = "Security Checkup",
                    subtitle = "Last scan: 2 hours ago",
                    textScale = textScale,
                    onClick = { Toast.makeText(context, "Scanning for threats...", Toast.LENGTH_SHORT).show() }
                )
            }

            // 4. Data & Communication
            SettingGroup(title = "Data & Communication", textScale = textScale) {
                SettingSwitchRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "Push Notifications",
                    subtitle = "Alerts for messages and updates",
                    checked = isNotificationEnabled,
                    textScale = textScale,
                    onCheckedChange = { 
                        isNotificationEnabled = it 
                        Toast.makeText(context, "Notifications ${if(it) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingClickableRow(
                    icon = Icons.Default.Storage,
                    title = "Storage & Data",
                    subtitle = "340 MB used / 1.2 GB free",
                    textScale = textScale,
                    onClick = { Toast.makeText(context, "Clearing cache...", Toast.LENGTH_SHORT).show() }
                )
            }

            // 5. Support
            SettingGroup(title = "Support", textScale = textScale) {
                SettingClickableRow(
                    icon = Icons.AutoMirrored.Filled.HelpCenter,
                    title = "Help Center",
                    subtitle = "Documentation and support",
                    textScale = textScale,
                    onClick = { Toast.makeText(context, "Redirecting to help center...", Toast.LENGTH_SHORT).show() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingClickableRow(
                    icon = Icons.Default.Policy,
                    title = "Privacy Policy",
                    subtitle = "Legal and data usage terms",
                    textScale = textScale,
                    onClick = { showPrivacyDialog = true }
                )
            }

            // 6. Action Buttons
            LogOutButton(textScale = textScale, onClick = { showLogoutDialog = true })

            // 7. Footer
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Setting Page v3.0.0",
                    fontWeight = FontWeight.Bold,
                    fontSize = (13 * textScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text(
                    text = "Made with ❤️ for MD SAIF",
                    fontSize = (11 * textScale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        if (showPrivacyDialog) {
            PrivacyDialog(textScale = textScale, onDismiss = { showPrivacyDialog = false })
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout Account", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to log out from your account?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            authViewModel.signOut(context) {
                                onLogout()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Logout", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showLockDialog) {
            LockSystemDialog(
                currentType = lockType,
                onTypeSelected = { type ->
                    if (type == "NONE") {
                        settingsViewModel.setLockType("NONE")
                        showLockDialog = false
                    } else {
                        pendingLockType = type
                        showLockDialog = false
                        showSetLockValueDialog = true
                    }
                },
                onDismiss = { showLockDialog = false }
            )
        }

        if (showSetLockValueDialog) {
            SetLockValueDialog(
                type = pendingLockType,
                onSave = { value ->
                    settingsViewModel.saveLockValue(value)
                    settingsViewModel.setLockType(pendingLockType)
                    showSetLockValueDialog = false
                    Toast.makeText(context, "$pendingLockType lock enabled", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showSetLockValueDialog = false }
            )
        }
    }
}

@Composable
fun SetLockValueDialog(type: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set $type") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Enter $type") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        confirmButton = {
            Button(onClick = { if (value.isNotBlank()) onSave(value) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LockSystemDialog(
    currentType: String,
    onTypeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Lock Type", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LockTypeItem("NONE", currentType == "NONE", onTypeSelected)
                LockTypeItem("PIN", currentType == "PIN", onTypeSelected)
                LockTypeItem("PATTERN", currentType == "PATTERN", onTypeSelected)
                LockTypeItem("PASSWORD", currentType == "PASSWORD", onTypeSelected)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LockTypeItem(type: String, isSelected: Boolean, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(type) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(type, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ProfileSection(name: String, email: String, photoUrl: String?, textScale: Float, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (name.isNotEmpty()) name.first().toString() else "?",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = (32 * textScale).sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name, 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = (22 * textScale).sp, 
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Verified, 
                            contentDescription = null, 
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = email, 
                        fontSize = (14 * textScale).sp, 
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    onClick = onEditClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingGroup(title: String, textScale: Float, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            fontSize = (12 * textScale).sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
            letterSpacing = 1.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            content = content
        )
    }
}

@Composable
fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    textScale: Float,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = (16 * textScale).sp)
                Text(subtitle, fontSize = (12 * textScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            thumbContent = if (checked) {
                { Icon(Icons.Default.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
            } else null
        )
    }
}

@Composable
fun SettingClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    textScale: Float,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = (16 * textScale).sp)
            Text(subtitle, fontSize = (12 * textScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun LogOutButton(textScale: Float, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.error
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text("Logout Account", fontWeight = FontWeight.ExtraBold, fontSize = (16 * textScale).sp)
    }
}

@Composable
fun PrivacyDialog(textScale: Float, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
        title = {
            Text(text = "Privacy Shield", fontWeight = FontWeight.Black, fontSize = (22 * textScale).sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🔒 End-to-end encryption for all your personal data.", fontSize = (14 * textScale).sp)
                Text("🚫 No third-party tracking or data selling, ever.", fontSize = (14 * textScale).sp)
                Text("✅ You have full control over your visibility settings.", fontSize = (14 * textScale).sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss, 
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I Understand", fontWeight = FontWeight.Bold)
            }
        }
    )
}
