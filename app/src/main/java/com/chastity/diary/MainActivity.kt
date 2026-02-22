package com.chastity.diary

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.chastity.diary.data.datastore.PreferencesManager
import com.chastity.diary.domain.model.DarkMode
import com.chastity.diary.domain.model.UserSettings
import com.chastity.diary.ui.navigation.BottomNavigationBar
import com.chastity.diary.ui.navigation.NavGraph
import com.chastity.diary.ui.screens.LockScreen
import com.chastity.diary.ui.screens.OnboardingScreen
import com.chastity.diary.ui.theme.DiaryTheme
import com.chastity.diary.util.BiometricHelper
import com.chastity.diary.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main activity
 */
class MainActivity : FragmentActivity() {
    private val _isLocked = MutableStateFlow(false)
    private val isLocked: StateFlow<Boolean> = _isLocked

    companion object {
        /**
         * 拍照前設為 true，讓 ON_STOP 不觸發鎖定。
         * 相機返回後的 ON_START 自動重置。
         */
        var isCameraLaunching: Boolean = false
    }

    // A-1/G-1: Startup data loaded off the main thread; null while loading (SplashScreen covers UI).
    private data class StartupData(
        val onboardingCompleted: Boolean,
        val userSettings: UserSettings,
        val lockEnabled: Boolean
    )
    private val _startupData = MutableStateFlow<StartupData?>(null)

    // F-1: In-memory lock flag so setupLifecycleObserver avoids synchronous AES decryption on main thread.
    private val _lockEnabled = MutableStateFlow(false)

    private lateinit var biometricHelper: BiometricHelper
    private lateinit var encryptedPrefs: android.content.SharedPreferences
    private lateinit var preferencesManager: PreferencesManager

    private var errorMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // A-2: installSplashScreen must be called before super.onCreate
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        biometricHelper    = BiometricHelper(this)
        preferencesManager = PreferencesManager(this)

        // A-1: Move all blocking crypto + DataStore I/O off the main thread.
        // G-1: Pre-fetch DataStore values so setContent never needs to show a loading spinner.
        lifecycleScope.launch(Dispatchers.IO) {
            // Run EncryptedPrefs init + two DataStore reads in parallel
            val encPrefsDeferred   = async {
                val masterKey = MasterKey.Builder(applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    applicationContext, Constants.ENCRYPTED_PREFS_NAME, masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }
            val onboardingDeferred = async { preferencesManager.isOnboardingCompleted.first() }
            val settingsDeferred   = async { preferencesManager.userSettingsFlow.first() }

            val prefs       = encPrefsDeferred.await()
            val lockEnabled = prefs.getBoolean(Constants.KEY_LOCK_ENABLED, false)
            val onboarding  = onboardingDeferred.await()
            val settings    = settingsDeferred.await()

            withContext(Dispatchers.Main) {
                encryptedPrefs     = prefs
                _lockEnabled.value = lockEnabled
                if (lockEnabled) _isLocked.value = true
                _startupData.value = StartupData(
                    onboardingCompleted = onboarding,
                    userSettings        = settings,
                    lockEnabled         = lockEnabled
                )
                setupLifecycleObserver()
            }
        }

        // A-2: Keep splash visible until all startup data is ready — eliminates the spinner flash
        splashScreen.setKeepOnScreenCondition { _startupData.value == null }

        setContent {
            val startupData by _startupData.collectAsState()
            val locked      by isLocked.collectAsState()
            // Live userSettings Flow for immediate dark-mode toggle after splash
            val liveSettings by preferencesManager.userSettingsFlow
                .collectAsState(initial = startupData?.userSettings ?: UserSettings())
            val systemIsDark = isSystemInDarkTheme()
            val darkTheme = when (liveSettings.darkMode) {
                DarkMode.LIGHT  -> false
                DarkMode.DARK   -> true
                DarkMode.SYSTEM -> systemIsDark
            }

            DiaryTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // startupData is null only while SplashScreen is showing — render nothing
                    val data = startupData ?: return@Surface

                    if (!data.onboardingCompleted) {
                        // B-1: onboarding not done — show onboarding only
                        OnboardingScreen(
                            onComplete = {
                                _startupData.value = data.copy(onboardingCompleted = true)
                            }
                        )
                    } else {
                        // PERF: Always compose MainScreen so all ViewModels/DB queries warm up
                        // before the user unlocks. LockScreen is overlaid on top when locked.
                        // This eliminates the "cold-compose" jank that appeared immediately after
                        // fingerprint/PIN unlock (all 4 KeepAliveScreens + ViewModels were
                        // freshly composed at the moment of unlock, blocking the main thread).
                        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                            MainScreen()
                            if (locked) {
                                // Opaque overlay — user cannot see or interact with MainScreen
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                ) {
                                    LockScreen(
                                        onUnlockWithBiometric = {
                                            biometricHelper.authenticate(
                                                activity = this@MainActivity,
                                                onSuccess = {
                                                    _isLocked.value = false
                                                    errorMessage = null
                                                },
                                                onError = { error -> errorMessage = error },
                                                onFailed = { errorMessage = getString(R.string.error_biometric_failed) }
                                            )
                                        },
                                        onUnlockWithPin = { pin ->
                                            val savedPin = encryptedPrefs.getString(Constants.KEY_PIN_CODE, "")
                                            if (pin == savedPin) {
                                                _isLocked.value = false
                                                errorMessage = null
                                            } else {
                                                errorMessage = getString(R.string.error_pin_incorrect)
                                            }
                                        },
                                        biometricAvailable = biometricHelper.isBiometricAvailable(),
                                        errorMessage = errorMessage
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        // F-1: Use in-memory flag — avoids main-thread AES decryption on every background event
                        if (_lockEnabled.value && !isCameraLaunching) {
                            _isLocked.value = true
                        }
                    }
                    Lifecycle.Event.ON_START -> {
                        isCameraLaunching = false
                    }
                    else -> {}
                }
            }
        )
    }
}

@Composable
fun MainScreen() {
    var currentRoute by remember { mutableStateOf(com.chastity.diary.ui.navigation.Screen.DailyEntry.route) }

    // H-3: System Back from any non-home tab navigates to DailyEntry instead of exiting the app
    androidx.activity.compose.BackHandler(
        enabled = currentRoute != com.chastity.diary.ui.navigation.Screen.DailyEntry.route
    ) {
        currentRoute = com.chastity.diary.ui.navigation.Screen.DailyEntry.route
    }

    Scaffold(
        bottomBar = {
            com.chastity.diary.ui.navigation.BottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { currentRoute = it }
            )
        }
    ) { paddingValues ->
        com.chastity.diary.ui.navigation.NavGraph(
            currentRoute = currentRoute,
            onNavigate = { currentRoute = it },
            outerPadding = paddingValues
        )
    }
}
