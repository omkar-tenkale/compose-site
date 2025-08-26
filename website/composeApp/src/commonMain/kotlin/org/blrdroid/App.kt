package org.blrdroid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

val supabase = createSupabaseClient(
    supabaseUrl = "https://gtmglyozinyruewnyina.supabase.co",
    supabaseKey = "sb_publishable_xnv7GhKlcmPEa-8mAjjeMw_SC-CLyFJ"
) {
    defaultSerializer = KotlinXSerializer(Json {
        ignoreUnknownKeys = true
    })
    install(Auth)
    install(Functions)
}

private const val EVENT_ID: Long = 1L

private enum class Screen {
    EventDetails, EventRegistration
}

@Composable
fun LoginPrompt() {
    val scope = rememberCoroutineScope()
    var launching by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Please log in to continue", style = MaterialTheme.typography.titleMedium)
        Button(
            enabled = !launching,
            onClick = {
                launching = true
                scope.launch { supabase.auth.signInWith(Github) }
            }
        ) {
            if (launching) {
                CircularProgressIndicator()
            } else {
                Text("Sign in with GitHub")
            }
        }
    }
}

@Preview()
@Composable
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(Screen.EventDetails) }
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = currentScreen == Screen.EventDetails,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                EventDetailsScreen(
                    onGoToRegistration = { currentScreen = Screen.EventRegistration }
                )
            }

            AnimatedVisibility(
                visible = currentScreen == Screen.EventRegistration,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                EventRegistrationScreen()
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    CenteredCard {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun EventDetailsScreen(
    onGoToRegistration: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf<EventDetailsData?>(null) }
    var unauthorized by remember { mutableStateOf(false) }

    fun retryLoad() {
        isLoading = true
        errorMessage = null
        details = null
        unauthorized = false
        // Launch side-effect to fetch
        // Using LaunchedEffect by bumping a key via state change
    }

    // Trigger initial and retry fetches by watching isLoading when details not set
    LaunchedEffect(isLoading) {
        if (isLoading) {
            try {
                val response = eventDetails(EVENT_ID)
                if (response.error != null) {
                    errorMessage = response.error?.message ?: "Unknown error"
                    details = null
                } else if (response.data == null) {
                    errorMessage = "No data returned"
                    details = null
                } else {
                    details = response.data
                }
            } catch (t: Throwable) {
                if(t is UnauthorizedRestException){
                    unauthorized = true
                    errorMessage = null
                    details = null
                } else {
                    errorMessage = t.message ?: t.toString()
                    details = null
                    t.printStackTrace()
                }
            } finally {
                isLoading = false
            }
        }
    }

    when {
        isLoading -> LoadingState()
        unauthorized -> LoginRequired { retryLoad() }
        errorMessage != null -> ErrorState(message = errorMessage!!) { retryLoad(); isLoading = true }
        details != null -> EventDetailsContent(details = details!!, onGoToRegistration = onGoToRegistration)
        else -> ErrorState(message = "Unexpected state") { retryLoad(); isLoading = true }
    }
}

@Composable
private fun LoginRequired(onRetry: () -> Unit) {
    CenteredCard {
        LoginPrompt()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onRetry) { Text("Retry loading event") }
        }
    }
}

@Composable
private fun EventDetailsContent(
    details: EventDetailsData,
    onGoToRegistration: () -> Unit
) {
    CenteredCard {
        Text(details.title, style = MaterialTheme.typography.headlineMedium)
        Text(details.eventName, style = MaterialTheme.typography.titleMedium)
        Text(details.date, style = MaterialTheme.typography.bodyMedium)
        Text(details.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onGoToRegistration) { Text("Registration and more") }
        }
    }
}

@Composable
private fun EventRegistrationScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var registration by remember { mutableStateOf<EventRegistrationDetailsData?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    fun triggerRefresh() {
        refreshKey++
        isLoading = true
        errorMessage = null
    }

    LaunchedEffect(refreshKey, isLoading) {
        if (isLoading) {
            try {
                val response = eventRegistrationDetails(EVENT_ID)
                if (response.error != null && response.error.code != "NOT_REGISTERED") {
                    errorMessage = response.error?.message ?: "Unknown error"
                    registration = null
                } else {
                    registration = response.data
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                errorMessage = t.message ?: t.toString()
                registration = null
            } finally {
                isLoading = false
            }
        }
    }

    val scope = rememberCoroutineScope()

    when {
        isLoading -> LoadingState()
        errorMessage != null -> ErrorState(message = errorMessage!!) { triggerRefresh() }
        else -> RegistrationContent(
            registration = registration,
            onRegister = {
                scope.launch {
                    try {
                        val res = registerForEvent(EVENT_ID)
                        if (res.error != null) {
                            errorMessage = res.error?.message ?: "Unknown error"
                        } else {
                            // success, refresh details to show confirmed status
                            triggerRefresh()
                        }
                    } catch (t: Throwable) {
                        errorMessage = t.message ?: t.toString()
                    }
                }
            },
            onCancel = {
                scope.launch {
                    try {
                        val res = cancelEventRegistration(EVENT_ID)
                        if (res.error != null) {
                            errorMessage = res.error?.message ?: "Unknown error"
                        } else {
                            triggerRefresh()
                        }
                    } catch (t: Throwable) {
                        errorMessage = t.message ?: t.toString()
                    }
                }
            }
        )
    }
}

@Composable
private fun RegistrationContent(
    registration: EventRegistrationDetailsData?,
    onRegister: () -> Unit,
    onCancel: () -> Unit
) {
    CenteredCard {
        when {
            registration?.isConfirmed() == true -> {
                Text("You're going!", style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onCancel) { Text("Cancel") }
                }
            }
            registration?.isCancelled() == true -> {
                Text("Registration cancelled.", style = MaterialTheme.typography.headlineSmall)
            }
            else -> {
                Text("You're not registered yet.", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onRegister) { Text("Click to register") }
                }
            }
        }
    }
}

@Composable
private fun CenteredCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}