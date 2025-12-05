package de.hipp.app.taskcards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Small helper to create a [ViewModelProvider.Factory] with a lambda.
 * Keeps Composables lean where we need a custom factory.
 */
fun <T : ViewModel> factoryOf(creator: () -> T): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}
