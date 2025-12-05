package de.hipp.app.taskcards.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import de.hipp.app.taskcards.model.TaskList
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed implementation of TaskListMetadataRepository.
 * Stores list metadata in Firestore for cloud synchronization.
 *
 * Data structure:
 * /users/{userId}/lists/{listId}
 *   - name: String
 *   - createdAt: Long
 *   - lastModifiedAt: Long
 *
 * @param firestore The Firestore instance
 * @param auth The Firebase Auth instance (defaults to FirebaseAuth.getInstance())
 */
class FirestoreTaskListMetadataRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : TaskListMetadataRepository {

    private val mutex = Mutex()

    companion object {
        private const val TAG = "FirestoreMetadataRepo"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_LISTS = "lists"
    }

    /**
     * Get reference to the lists collection for the current user.
     * Returns null if user is not authenticated.
     */
    private fun getListsCollection() =
        auth.currentUser?.uid?.let { userId ->
            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_LISTS)
        }

    override fun observeTaskLists(): Flow<List<TaskList>> = callbackFlow {
        val listsCollection = getListsCollection()
        if (listsCollection == null) {
            Log.w(TAG, "User not authenticated, cannot observe lists")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = listsCollection
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing task lists", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val lists = snapshot.documents.mapNotNull { doc ->
                        try {
                            TaskList(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                lastModifiedAt = doc.getLong("lastModifiedAt") ?: 0L
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing list document ${doc.id}", e)
                            null
                        }
                    }
                    trySend(lists)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun createList(name: String): String =
        mutex.withLock {
            val trimmedName = name.trim()
            require(trimmedName.isNotEmpty()) { "List name cannot be empty" }
            require(trimmedName.length <= 100) { "List name cannot exceed 100 characters" }

            val listsCollection = getListsCollection()
                ?: throw IllegalStateException("User not authenticated")

            try {
                val docRef = listsCollection.document()
                val now = System.currentTimeMillis()

                val listData = hashMapOf(
                    "name" to trimmedName,
                    "createdAt" to now,
                    "lastModifiedAt" to now
                )

                docRef.set(listData).await()
                Log.d(TAG, "Created list ${docRef.id} with name '$trimmedName'")

                docRef.id
            } catch (e: Exception) {
                Log.e(TAG, "Error creating list", e)
                throw e
            }
        }

    override suspend fun renameList(listId: String, newName: String): Unit =
        mutex.withLock {
            val trimmedName = newName.trim()
            require(trimmedName.isNotEmpty()) { "List name cannot be empty" }
            require(trimmedName.length <= 100) { "List name cannot exceed 100 characters" }

            val listsCollection = getListsCollection()
                ?: throw IllegalStateException("User not authenticated")

            try {
                val updates = hashMapOf<String, Any>(
                    "name" to trimmedName,
                    "lastModifiedAt" to System.currentTimeMillis()
                )

                listsCollection.document(listId)
                    .update(updates)
                    .await()

                Log.d(TAG, "Renamed list $listId to '$trimmedName'")
            } catch (e: Exception) {
                Log.e(TAG, "Error renaming list $listId", e)
                throw e
            }
        }

    override suspend fun deleteList(listId: String): Unit =
        mutex.withLock {
            val listsCollection = getListsCollection()
                ?: throw IllegalStateException("User not authenticated")

            try {
                // Delete the list document (tasks will be in subcollection)
                // Note: In Firestore, deleting a document doesn't delete its subcollections
                // For production, you'd want a Cloud Function to clean up tasks
                listsCollection.document(listId).delete().await()
                Log.d(TAG, "Deleted list $listId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting list $listId", e)
                throw e
            }
        }

    override suspend fun getDefaultListId(): String? =
        mutex.withLock {
            val listsCollection = getListsCollection()
            if (listsCollection == null) {
                Log.w(TAG, "User not authenticated, cannot get default list")
                return@withLock null
            }

            try {
                val snapshot = listsCollection
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .limit(1)
                    .get()
                    .await()

                snapshot.documents.firstOrNull()?.id
            } catch (e: Exception) {
                Log.e(TAG, "Error getting default list ID", e)
                null
            }
        }
}
