package pl.luczak.m.serverapplication.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import pl.luczak.m.serverapplication.MainActivity
import pl.luczak.m.serverapplication.R
import pl.luczak.m.serverapplication.utils.Operation
import pl.luczak.m.serverapplication.utils.ServiceTriggerState
import pl.luczak.m.serverapplication.utils.setState
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class PermanentService : Service(), CoroutineScope {
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private val TAG = "PermanentService"

    private var server: HttpServer? = null
    var job: Job = Job()

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                Operation.START.name -> startService()
                Operation.STOP.name -> stopService()
                else -> {
                }
            }
        } else {
            // null intent, probably restarted by the system
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "service destroyed")
    }

    private fun startService() {
        if (isServiceStarted) return
        isServiceStarted = true
        Log.i(TAG, "service starting")
        setState(this, ServiceTriggerState.STARTED)

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PermanentService::lock").apply { acquire() }
        }

        startServer()
    }

    private fun startServer() {
        try {
            server = HttpServer.create(InetSocketAddress(8080), 0)
            server!!.executor = Executors.newCachedThreadPool()
            server!!.createContext("/testMethod", rootHandler)
            server!!.start()
            Log.i(TAG, "server starts")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopService() {
        Log.i(TAG, "service stopping")
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
        }
        isServiceStarted = false
        setState(this, ServiceTriggerState.STOPPED)
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "ANDROID SERVER CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "Android server notifications channel",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Android server channel"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 200, 300)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val builder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                this,
                notificationChannelId
            ) else Notification.Builder(this)

        return builder
            .setContentTitle("ANDROID SERVER")
            .setContentText("Server is running")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(Notification.PRIORITY_HIGH) //under android 26 compatibility
            .build()
    }

    private val rootHandler = HttpHandler { exchange ->
        run {
            when (exchange!!.requestMethod) {
                "GET" -> {
                    sendResponseText(exchange, "WELCOME TO ANDROID SERVER!")
                }
                "POST" -> {
                    launch {
                        val inputStream = exchange.requestBody
                        val requestBody = streamToString(inputStream)
                        val jsonBody = JSONObject(requestBody)
                        //should set field name here
                        val field = jsonBody.get("")
                        sendResponse(exchange, field.toString())
                    }
                }
                else -> {
                } //error
            }
        }
    }

    private fun sendResponseText(exchange: HttpExchange, text: String) {
        val os = exchange.responseBody
        exchange.sendResponseHeaders(200, text.length.toLong())
        os.write(text.toByteArray())
        os.close()
    }

    private fun sendResponse(exchange: HttpExchange, responseText: String) {
        val json = "{\n\"hello\": \"${responseText}\",\n\"hello\": \"world\"\n}"
        val os = exchange.responseBody
        exchange.responseHeaders.set("Content-Type", "appication/json");
        exchange.sendResponseHeaders(200, json.length.toLong())
        os.write(json.toByteArray())
        os.close()
    }

    private fun streamToString(inputStream: InputStream): String {
        val s = Scanner(inputStream).useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }
}