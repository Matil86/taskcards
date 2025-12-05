package de.hipp.app.taskcards.auth

import android.content.Intent
import kotlinx.coroutines.flow.Flow

/**
 * Authentication service abstraction.
 * Provides user ID for multi-user support in Firestore.
 */
interface AuthService {
    /**
     * Get the current user ID, or null if not authenticated.
     */
    suspend fun getCurrentUserId(): String?

    /**
     * Get the current user's email, or null if not authenticated.
     */
    suspend fun getCurrentUserEmail(): String?

    /**
     * Get the current user's display name, or null if not authenticated.
     */
    suspend fun getCurrentUserDisplayName(): String?

    /**
     * Observe authentication state changes.
     * Emits user ID when authenticated, null when signed out.
     */
    fun observeAuthState(): Flow<String?>

    /**
     * Get the Google Sign-In intent.
     * Launch this intent using startActivityForResult.
     */
    suspend fun getGoogleSignInIntent(): Intent

    /**
     * Handle the result from Google Sign-In activity.
     * Call this from onActivityResult.
     * Returns the user ID on success.
     */
    suspend fun handleGoogleSignInResult(data: Intent?): String

    /**
     * Sign out the current user.
     */
    suspend fun signOut()

    /**
     * Check if user is currently authenticated.
     */
    suspend fun isAuthenticated(): Boolean
}
