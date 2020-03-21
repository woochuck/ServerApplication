package pl.luczak.m.serverapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import pl.luczak.m.serverapplication.service.PermanentService
import pl.luczak.m.serverapplication.utils.Operation
import pl.luczak.m.serverapplication.utils.ServiceTriggerState
import pl.luczak.m.serverapplication.utils.getState

class StartingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            getState(context) == ServiceTriggerState.STARTED) {
            Intent(context, PermanentService::class.java).also {
                it.action = Operation.START.name
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(it)
                    return
                }
                context.startService(it)
            }
        }
    }
}