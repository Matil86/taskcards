package de.hipp.app.taskcards.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
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
 * /lists/{listId}
 *   - creatorUid: String
 *   - contributors: List<String>  (array of UIDs; enables whereArrayContains queries)
 *   - name: String
 *   - createdAt: Long             (milliseconds since epoch)
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
        private const val COLLECTION_LISTS = "lists"
    }

    override fun observeTaskLists(): Flow<List<TaskList>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "User not authenticated, cannot observe lists")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection(COLLECTION_LISTS)
            .whereArrayContains("contributors", userId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100) // prevent unbounded reads for users with many lists
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing task lists", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val lists = snapshot.documents.mapNotNull { doc ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            TaskList(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                lastModifiedAt = doc.getLong("lastModifiedAt") ?: 0L,
                                creatorUid = doc.getString("creatorUid") ?: "",
                                contributors = (doc.get("contributors") as? List<String>) ?: emptyList()
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

            val uid = auth.currentUser?.uid
                ?: throw IllegalStateException("User not authenticated")

            try {
                val docRef = firestore.collection(COLLECTION_LISTS).document(UUID.randomUUID().toString())
                val now = System.currentTimeMillis()

                val listData = hashMapOf(
                    "creatorUid" to uid,
                    "contributors" to listOf(uid),
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

            try {
                firestore.collection(COLLECTION_LISTS).document(listId)
                    .update(mapOf("name" to trimmedName, "lastModifiedAt" to System.currentTimeMillis()))
                    .await()

                Log.d(TAG, "Renamed list $listId to '$trimmedName'")
            } catch (e: Exception) {
                Log.e(TAG, "Error renaming list $listId", e)
                throw e
            }
        }

    override suspend fun deleteList(listId: String): Unit =
        mutex.withLock {
            try {
                // Delete the list document.
                // Note: Firestore does not delete subcollections automatically.
                // Task cleanup is handled by a Cloud Function in production.
                firestore.collection(COLLECTION_LISTS).document(listId).delete().await()
                Log.d(TAG, "Deleted list $listId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting list $listId", e)
                throw e
            }
        }

    override suspend fun ensureListExists(listId: String, name: String): Unit =
        mutex.withLock {
            val uid = auth.currentUser?.uid ?: return@withLock
            val doc = firestore.collection(COLLECTION_LISTS).document(listId)
            val snapshot = doc.get().await()
            if (!snapshot.exists()) {
                val now = System.currentTimeMillis()
                doc.set(hashMapOf(
                    "creatorUid" to uid,
                    "contributors" to listOf(uid),
                    "name" to name,
                    "createdAt" to now,
                    "lastModifiedAt" to now
                )).await()
                Log.d(TAG, "Created list document for $listId with name '$name'")
            }
        }

    override suspend fun getDefaultListId(): String? =
        mutex.withLock {
            val userId = auth.currentUser?.uid ?: run {
                Log.w(TAG, "User not authenticated, cannot get default list")
                return@withLock null
            }

            try {
                firestore.collection(COLLECTION_LISTS)
                    .whereArrayContains("contributors", userId)
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .limit(1)
                    .get()
                    .await()
                    .documents.firstOrNull()?.id
            } catch (e: Exception) {
                Log.e(TAG, "Error getting default list ID", e)
                null
            }
        }
}
