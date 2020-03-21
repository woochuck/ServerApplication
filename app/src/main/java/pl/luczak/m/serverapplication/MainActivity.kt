package pl.luczak.m.serverapplication

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import pl.luczak.m.serverapplication.service.PermanentService
import pl.luczak.m.serverapplication.utils.Operation
import pl.luczak.m.serverapplication.utils.Operation.*
import pl.luczak.m.serverapplication.utils.ServiceTriggerState
import pl.luczak.m.serverapplication.utils.getState

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViews()
    }

    private fun findViews() {
        findViewById<Button>(R.id.btnStart).let {
            it.setOnClickListener {
                serviceOperation(START)
            }
        }

        findViewById<Button>(R.id.btnStop).let {
            it.setOnClickListener {
                serviceOperation(STOP)
            }
        }
    }

    private fun serviceOperation(operation: Operation) {
        if (getState(this) == ServiceTriggerState.STOPPED && operation == STOP) return
        Intent(this, PermanentService::class.java).also {
            it.action = operation.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it)
                return
            }
            startService(it)
        }
    }
}
