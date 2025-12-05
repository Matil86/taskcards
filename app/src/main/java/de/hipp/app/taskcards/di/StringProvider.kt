package de.hipp.app.taskcards.di

import android.content.Context
import androidx.annotation.StringRes

/**
 * Provides access to string resources for ViewModels.
 * This interface allows ViewModels to remain Android framework-agnostic
 * while still accessing localized strings.
 */
interface StringProvider {
    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
}

/**
 * Android implementation of StringProvider using Context.
 */
class AndroidStringProvider(private val context: Context) : StringProvider {
    override fun getString(resId: Int): String {
        return context.getString(resId)
    }

    override fun getString(resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }
}
