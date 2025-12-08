package de.hipp.app.taskcards.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import de.hipp.app.taskcards.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of AuthService.
 * Uses Google Credential Manager API for authentication with device Google account.
 */
class FirebaseAuthService(private val context: Context) : AuthService {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    companion object {
        private const val TAG = "FirebaseAuthService"
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

    override suspend fun signInWithGoogle(activityContext: Context): String {
        return try {
            Log.d(TAG, "Starting Google Sign-In with Credential Manager")

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            handleSignIn(result)
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            val errorMessage = when (e) {
                is androidx.credentials.exceptions.GetCredentialCancellationException ->
                    "Sign-in was cancelled"
                is androidx.credentials.exceptions.NoCredentialException ->
                    "No Google account found on device"
                is androidx.credentials.exceptions.GetCredentialInterruptedException ->
                    "Sign-in was interrupted"
                else -> "Google Sign-In failed: ${e.message}"
            }
            Log.e(TAG, errorMessage, e)
            throw de.hipp.app.taskcards.exception.AuthenticationException(errorMessage, e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during Google Sign-In", e)
            throw e
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse): String {
        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

            val googleIdTokenCredential = GoogleIdTokenCredential
                .createFrom(credential.data)

            Log.d(TAG, "Authenticating with Firebase using Google account: ${googleIdTokenCredential.id}")

            val firebaseCredential = GoogleAuthProvider
                .getCredential(googleIdTokenCredential.idToken, null)

            val authResult = auth.signInWithCredential(firebaseCredential).await()

            val userId = authResult.user?.uid
                ?: throw IllegalStateException("Failed to get user ID after Google Sign-In")

            Log.d(TAG, "Successfully signed in with Google. User ID: $userId, Email: ${authResult.user?.email}")
            return userId
        }

        throw IllegalStateException("Unexpected credential type: ${credential.type}")
    }

    override suspend fun signOut() {
        try {
            // Sign out from Firebase
            auth.signOut()

            // Clear credential state from Credential Manager
            credentialManager.clearCredentialState(
                ClearCredentialStateRequest()
            )

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
