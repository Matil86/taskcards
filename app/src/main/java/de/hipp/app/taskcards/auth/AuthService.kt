package de.hipp.app.taskcards.auth

import android.content.Context
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
     * Sign in with Google using Credential Manager API.
     * Returns the user ID on success.
     * @param activityContext The activity context required for launching the credential picker UI
     */
    suspend fun signInWithGoogle(activityContext: Context): String

    /**
     * Sign out the current user.
     */
    suspend fun signOut()

    /**
     * Check if user is currently authenticated.
     */
    suspend fun isAuthenticated(): Boolean
}
