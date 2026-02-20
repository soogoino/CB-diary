package com.chastity.diary

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.chastity.diary.data.datastore.PreferencesManager
import com.chastity.diary.ui.navigation.BottomNavigationBar
import com.chastity.diary.ui.navigation.NavGraph
import com.chastity.diary.ui.screens.LockScreen
import com.chastity.diary.ui.screens.OnboardingScreen
import com.chastity.diary.ui.theme.DiaryTheme
import com.chastity.diary.utils.BiometricHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Main activity
 */
class MainActivity : FragmentActivity() {
    private val _isLocked = MutableStateFlow(false)
    private val isLocked: StateFlow<Boolean> = _isLocked

    private lateinit var biometricHelper: BiometricHelper
    private lateinit var encryptedPrefs: android.content.SharedPreferences
    private lateinit var preferencesManager: PreferencesManager

    private var errorMessage by mutableStateOf<String?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize biometric helper
        biometricHelper = BiometricHelper(this)

        // Initialize preferences manager
        preferencesManager = PreferencesManager(this)

        // Initialize encrypted preferences
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        encryptedPrefs = EncryptedSharedPreferences.create(
            this,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        // Check if lock is enabled
        val lockEnabled = encryptedPrefs.getBoolean("lock_enabled", false)
        if (lockEnabled) {
            _isLocked.value = true
        }
        
        // Setup lifecycle observer for app backgrounding
        setupLifecycleObserver()
        
        setContent {
            val locked by isLocked.collectAsState()
            val onboardingCompleted by preferencesManager.isOnboardingCompleted
                .collectAsState(initial = null)

            DiaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        // Still loading DataStore
                        onboardingCompleted == null -> {
                            androidx.compose.foundation.layout.Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                        // Onboarding not yet completed
                        onboardingCompleted == false -> {
                            OnboardingScreen()
                        }
                        // Locked
                        locked -> {
                            LockScreen(
                                onUnlockWithBiometric = {
                                    biometricHelper.authenticate(
                                        activity = this@MainActivity,
                                        onSuccess = {
                                            _isLocked.value = false
                                            errorMessage = null
                                        },
                                        onError = { error -> errorMessage = error },
                                        onFailed = { errorMessage = "辨識失敗，請重試" }
                                    )
                                },
                                onUnlockWithPin = { pin ->
                                    val savedPin = encryptedPrefs.getString("pin_code", "")
                                    if (pin == savedPin) {
                                        _isLocked.value = false
                                        errorMessage = null
                                    } else {
                                        errorMessage = "PIN 碼錯誤"
                                    }
                                },
                                biometricAvailable = biometricHelper.isBiometricAvailable(),
                                errorMessage = errorMessage
                            )
                        }
                        // All good → main app
                        else -> MainScreen()
                    }
                }
            }
        }
    }
    
    private fun setupLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                val lockEnabled = encryptedPrefs.getBoolean("lock_enabled", false)
                if (lockEnabled) {
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            // App went to background
                            lifecycleScope.launch {
                                _isLocked.value = true
                            }
                        }
                        else -> {}
                    }
                }
            }
        )
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        NavGraph(navController = navController, outerPadding = paddingValues)
    }
}
