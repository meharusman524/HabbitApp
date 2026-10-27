package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.HabitRepository
import com.example.ui.auth.AuthManager
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.HabitualTheme
import com.example.ui.viewmodel.HabitViewModel
import com.example.ui.viewmodel.HabitViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HabitualTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var showSignInScreen by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                showSignInScreen = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Silent background auto sign-in attempt if user previously had credentials
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    showSignInScreen = false
                },
                onUnauthenticated = { /* Keep on Home screen as guest */ },
                scope = coroutineScope
            )
        }
    }

    val repoKey = currentUser?.uid ?: "guest"
    val repository = remember(repoKey) { HabitRepository(context) }
    val habitViewModel: HabitViewModel = viewModel(
        key = repoKey,
        factory = HabitViewModelFactory(repository)
    )

    // When user signs in, seamlessly sync local records to Firestore
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            repository.syncLocalHabitsToCloud()
        }
    }

    // ALWAYS open directly to the Home screen for everyone
    MainAppScreen(
        viewModel = habitViewModel,
        isGuestMode = (currentUser == null),
        onSignInRequested = {
            showSignInScreen = true
        },
        onSignOut = {
            AuthManager.signOut(
                context = context,
                credentialManager = credentialManager,
                onSignOutComplete = {
                    currentUser = null
                    showSignInScreen = false
                },
                scope = coroutineScope
            )
        }
    )

    // Sign-in screen is completely optional and non-intrusive
    if (showSignInScreen && currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                showSignInScreen = false
            },
            onContinueAsGuest = {
                showSignInScreen = false
            },
            onDismiss = {
                showSignInScreen = false
            }
        )
    }
}
