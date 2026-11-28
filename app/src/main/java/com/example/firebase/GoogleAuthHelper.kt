package com.example.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val activity = context as? Activity
    if (activity == null) {
        onAuthError("Unable to open Google Sign-In from current screen")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutGoogle(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

fun signInOrRegisterWithEmail(
    email: String,
    password: String,
    createAccount: Boolean,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
        onAuthError("Please enter a valid email address (Gmail, Zoho Mail, or work email).")
        return
    }
    if (password.length < 6) {
        onAuthError("Password must be at least 6 characters.")
        return
    }
    scope.launch {
        try {
            if (createAccount) {
                Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            } else {
                Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
            }
            onAuthSuccess()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Email/Zoho auth failed", e)
            onAuthError(e.localizedMessage ?: "Email sign-in failed. Ensure Email/Password auth is enabled or use Google Sign-In.")
        }
    }
}

fun startPhoneOtpVerification(
    context: Context,
    rawPhoneNumber: String,
    onCodeSent: (verificationId: String) -> Unit,
    onAutoVerified: () -> Unit,
    onAuthError: (String) -> Unit
) {
    val activity = context as? Activity
    if (activity == null) {
        onAuthError("Unable to start Phone OTP from this screen.")
        return
    }
    val cleaned = rawPhoneNumber.trim().replace(" ", "")
    if (!cleaned.startsWith("+") || cleaned.length < 10) {
        onAuthError("Enter phone with country code (e.g., +919876543210).")
        return
    }
    try {
        val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(cleaned)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    Firebase.auth.signInWithCredential(credential)
                        .addOnSuccessListener { onAutoVerified() }
                        .addOnFailureListener { err ->
                            onAuthError(err.localizedMessage ?: "Phone verification failed.")
                        }
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    Log.e(AUTH_TAG, "Phone OTP verification failed", e)
                    onAuthError(e.localizedMessage ?: "Phone OTP failed. Ensure Phone Auth is enabled in Firebase Console.")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
                ) {
                    onCodeSent(verificationId)
                }
            })
            .build()
        com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "Phone OTP launch error", e)
        onAuthError(e.localizedMessage ?: "Could not start Phone OTP verification.")
    }
}

fun verifyPhoneOtpCode(
    verificationId: String,
    smsCode: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    val cleanCode = smsCode.trim()
    if (cleanCode.length < 4) {
        onAuthError("Enter the 6-digit OTP sent to your phone.")
        return
    }
    scope.launch {
        try {
            val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, cleanCode)
            Firebase.auth.signInWithCredential(credential).await()
            onAuthSuccess()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "OTP code verification failed", e)
            onAuthError(e.localizedMessage ?: "Invalid or expired OTP code.")
        }
    }
}

fun signInWithGitHub(
    context: Context,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    val activity = context as? Activity
    if (activity == null) {
        onAuthError("Unable to open GitHub Sign-In from current screen.")
        return
    }
    val provider = com.google.firebase.auth.OAuthProvider.newBuilder("github.com")
    val pending = Firebase.auth.pendingAuthResult
    if (pending != null) {
        pending
            .addOnSuccessListener { onAuthSuccess() }
            .addOnFailureListener { e ->
                Log.e(AUTH_TAG, "GitHub pending auth failed", e)
                onAuthError(e.localizedMessage ?: "GitHub sign-in failed.")
            }
    } else {
        Firebase.auth.startActivityForSignInWithProvider(activity, provider.build())
            .addOnSuccessListener { onAuthSuccess() }
            .addOnFailureListener { e ->
                Log.e(AUTH_TAG, "GitHub sign-in failed", e)
                onAuthError(e.localizedMessage ?: "GitHub sign-in failed. Enable GitHub provider in Firebase Auth or use Google Sign-In.")
            }
    }
}

