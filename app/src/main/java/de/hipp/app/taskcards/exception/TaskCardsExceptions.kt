package de.hipp.app.taskcards.exception

/**
 * Base exception for TaskCards application errors
 */
sealed class TaskCardsException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when a repository operation fails
 */
class RepositoryException(message: String, cause: Throwable? = null) : TaskCardsException(message, cause)

/**
 * Exception thrown when Firebase/Firestore operations fail
 */
class FirestoreException(message: String, cause: Throwable? = null) : TaskCardsException(message, cause)

/**
 * Exception thrown when authentication operations fail
 */
class AuthenticationException(message: String, cause: Throwable? = null) : TaskCardsException(message, cause)

/**
 * Exception thrown when deep link parsing fails
 */
class DeepLinkException(message: String, cause: Throwable? = null) : TaskCardsException(message, cause)

/**
 * Exception thrown when QR code operations fail
 */
class QRCodeException(message: String, cause: Throwable? = null) : TaskCardsException(message, cause)

/**
 * Exception thrown when worker operations fail
 */
class WorkerException(message: String, cause: Throwable? = null) : TaskCardsException(message, cause)

/**
 * Exception thrown when data serialization/deserialization fails
 */
class SerializationException(message: String, cause: Throwable? = null) : TaskCardsException(message, cause)

/**
 * Exception thrown when widget operations fail
 */
class WidgetException(message: String, cause: Throwable? = null) : TaskCardsException(message, cause)
