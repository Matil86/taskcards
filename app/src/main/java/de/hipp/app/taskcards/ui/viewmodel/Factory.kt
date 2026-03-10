package de.hipp.app.taskcards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Small helper to create a [ViewModelProvider.Factory] with a lambda.
 * Keeps Composables lean where we need a custom factory.
 */
fun <T : ViewModel> factoryOf(creator: () -> T): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    // Safe cast: the lambda `creator` always produces exactly T, and the caller supplies T
    // at the call-site via the reified type parameter. The cast to VM is therefore always
    // correct — any mismatch would be a programmer error caught at compile time.
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}
