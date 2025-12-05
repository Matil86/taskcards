package de.hipp.app.taskcards.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import de.hipp.app.taskcards.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of AuthService.
 * Uses Google Sign-In for authentication with device Google account.
 */
class FirebaseAuthService(private val context: Context) : AuthService {
    private val auth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient

    companion object {
        private const val TAG = "FirebaseAuthService"
    }

    init {
        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    override suspend fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override suspend fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    override suspend fun getCurrentUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }

    override fun observeAuthState(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)

        // Emit current state immediately
        trySend(auth.currentUser?.uid)

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    override suspend fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    override suspend fun handleGoogleSignInResult(data: Intent?): String {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            if (account == null) {
                throw IllegalStateException("Google Sign-In account is null")
            }

            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                10 -> "Developer Error (10): SHA-1 fingerprint mismatch or incorrect OAuth client configuration. Please add your SHA-1 to Firebase Console."
                12500 -> "Sign-in failed (12500): Please check that Google Sign-In is enabled in Firebase Authentication."
                7 -> "Network Error (7): Please check your internet connection."
                else -> "Google Sign-In failed with code ${e.statusCode}: ${e.message}"
            }
            Log.e(TAG, errorMessage, e)
            throw Exception(errorMessage, e)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling Google Sign-In result", e)
            throw e
        }
    }

    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): String {
        Log.d(TAG, "Authenticating with Firebase using Google account: ${account.email}")

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val result = auth.signInWithCredential(credential).await()

        val userId = result.user?.uid
            ?: throw IllegalStateException("Failed to get user ID after Google Sign-In")

        Log.d(TAG, "Successfully signed in with Google. User ID: $userId, Email: ${result.user?.email}")
        return userId
    }

    override suspend fun signOut() {
        try {
            // Sign out from both Firebase and Google
            auth.signOut()
            googleSignInClient.signOut().await()
            Log.d(TAG, "Signed out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
            throw e
        }
    }

    override suspend fun isAuthenticated(): Boolean {
        return auth.currentUser != null
    }
}
