package com.example.project

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.project.database.AppDatabase
import com.example.project.database.User
import com.example.project.database.UserDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jitsi.meet.sdk.JitsiMeet
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var lightLevel by mutableStateOf(0f)
    private var lastDismissedTime by mutableStateOf(-300000L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        setContent {
            ProjectTheme {
                AppNavigation(lightLevel, ::isWarningAllowed, ::resetWarningTimer)
            }
        }
    }
    private fun resetWarningTimer() {
        lastDismissedTime = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.also { light ->
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            lightLevel = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun isWarningAllowed(): Boolean {
        return System.currentTimeMillis() - lastDismissedTime > TimeUnit.MINUTES.toMillis(5)
    }
}

@Composable
fun ProjectTheme(content: @Composable () -> Unit) {
    val defaultFontFamily = FontFamily.SansSerif
    val typography = Typography(
        displayLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 57.sp
        ),
        displayMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp
        ),
        displaySmall = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp
        ),
        bodySmall = TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp
        )
    )
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xff0179ae),
            secondary = Color(0xFF03DAC5),
            onPrimary = Color.White,
            onSecondary = Color.Black
        ),
        typography = typography,
        content = content
    )
}

@Composable
fun AppNavigation(lightLevel: Float, isWarningAllowed: () -> Boolean, onDismissWarning: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val appDatabase = remember { AppDatabase.getDatabase(context) }
    val userDao = remember { appDatabase.userDao() }

    NavHost(navController = navController, startDestination = "signup") {
        composable("signup") {
            SignUpScreen(userDao, onSignUpComplete = {
                navController.navigate("login")
            }, onSkipClicked = {
                navController.navigate("login")
            })
            LightWarning(lightLevel, isWarningAllowed, onDismissWarning)
        }
        composable("login") {
            LoginScreen(userDao, navController)
            LightWarning(lightLevel, isWarningAllowed, onDismissWarning)
        }
        composable("main/{username}") { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            MainScreen(username, navController)
            LightWarning(lightLevel, isWarningAllowed, onDismissWarning)
        }
    }
}


@Composable
fun LightWarning(lightLevel: Float, isWarningAllowed: () -> Boolean, onDismissWarning: () -> Unit) {
    if (lightLevel < 100 && isWarningAllowed()) { // Assuming 100 Lux is the threshold
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Warning") },
            text = { Text("Light level is low, which may cause eye strain. Please increase room lighting.") },
            confirmButton = {
                Button(onClick = {
                    onDismissWarning()
                }) {
                    Text("OK")
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(userDao: UserDao, onSignUpComplete: () -> Unit, onSkipClicked: () -> Unit) {
    var userName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Image(
                painter = painterResource(id = R.drawable.user_icon),
                contentDescription = "Profile Placeholder",
                modifier = Modifier
                    .size(150.dp)
                    .padding(bottom = 16.dp)
            )
        }
        item {
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Username") },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.outlinedTextFieldColors()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Row {
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            userDao.insert(User(userName = userName, password = password))
                            withContext(Dispatchers.Main) {
                                onSignUpComplete()
                            }
                        }
                    }
                ) {
                    Text("Sign Up")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onSkipClicked() }) {
                    Text("Skip")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(userDao: UserDao, navController: NavController) {
    var userName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showIncorrectCredentialsDialog by remember { mutableStateOf(false) } // State to manage dialog visibility

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Image(
                painter = painterResource(id = R.drawable.user_icon),
                contentDescription = "Profile Placeholder",
                modifier = Modifier
                    .size(150.dp)
                    .padding(bottom = 16.dp)
            )
        }
        item {
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Username") },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.outlinedTextFieldColors()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val user = userDao.getUser(userName, password)
                        withContext(Dispatchers.Main) {
                            if (user != null) {
                                navController.navigate("main/${user.userName}")
                            } else {
                                showIncorrectCredentialsDialog = true // Show dialog if login fails
                            }
                        }
                    }
                }
            ) {
                Text("Login")
            }
        }
    }

    if (showIncorrectCredentialsDialog) {
        AlertDialog(
            onDismissRequest = { showIncorrectCredentialsDialog = false },
            title = { Text("Login Failed") },
            text = { Text("Incorrect Username or Password. Please try again.") },
            confirmButton = {
                Button(onClick = { showIncorrectCredentialsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(username: String, navController: NavController) {
    var roomName by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Image(
                painter = painterResource(id = R.drawable.user_icon),
                contentDescription = "Profile Placeholder",
                modifier = Modifier
                    .size(150.dp)
                    .padding(bottom = 16.dp)
            )
            Text("Welcome, $username!")
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            OutlinedTextField(
                value = roomName,
                onValueChange = { roomName = it },
                label = { Text("Enter the Room Name") },
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            StartCallButton(username, roomName)
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Button(onClick = {
                // Navigate back to the signup or login screen
                navController.popBackStack(navController.graph.startDestinationId, false)
            }) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun StartCallButton(username: String, roomName: String) {
    val context = LocalContext.current
    Button(onClick = {
        val options = JitsiMeetConferenceOptions.Builder()
            .setRoom(roomName)
            .setUserInfo(JitsiMeetUserInfo(Bundle().apply {
                putString("displayName", username)
            }))
            .build()
        JitsiMeetActivity.launch(context, options)
    }) {
        Text("Join Call")
    }
}
