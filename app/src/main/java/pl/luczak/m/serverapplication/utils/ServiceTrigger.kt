package pl.luczak.m.serverapplication.utils

import android.content.Context
import android.content.SharedPreferences

enum class ServiceTriggerState {
    STARTED, STOPPED,
}

private const val name = "SERVICE_KEY"
private const val key = "SERVICE_STATE"

fun setState(context: Context, state: ServiceTriggerState) {
    val preferences = getPreferences(context)
    preferences.edit().let {
        it.putString(key, state.name)
        it.apply()
    }
}

fun getState(context: Context): ServiceTriggerState {
    val sharedPrefs = getPreferences(context)
    val value = sharedPrefs.getString(key, ServiceTriggerState.STOPPED.name)
    return ServiceTriggerState.valueOf(value!!)
}

private fun getPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(name, 0)
}