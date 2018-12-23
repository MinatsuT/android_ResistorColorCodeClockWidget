package com.example.minatsu.rccclock

import android.app.Notification
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.widget.RemoteViews
import android.text.Html
import android.util.Log
import android.app.Service
import android.content.*
import android.os.IBinder
import android.os.Build
import android.os.Handler
import android.support.v4.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*
import android.app.NotificationManager
import android.app.NotificationChannel
import android.graphics.Color


/**
 * Implementation of App Widget functionality.
 */
class ResistorColorCodeClockWidget : AppWidgetProvider() {
    private val TAG = ResistorColorCodeClockWidget::class.java.simpleName

    private fun startService(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(Intent(context, MyService::class.java))
        } else {
            context.startService(Intent(context, MyService::class.java))
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive ${intent}")
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate")
        startService(context)
    }

    override fun onEnabled(context: Context) {
        Log.d(TAG, "Enabled ${context}")
        startService(context)
    }

    override fun onDisabled(context: Context) {
        Log.d(TAG, "Disabled ${context}")
        context.stopService(Intent(context, MyService::class.java))
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        Log.d(TAG, "Deleted")
    }

    class MyService : Service() {
        private val TAG = MyService::class.java.simpleName
        private val ACTION_START_MY_ALARM = "com.example.minatsu.r_colorclock.ResistorColorCodeClockWidget.MyService.ACTION_START_MY_ALARM"
        private val mCol = arrayOf("#000000", "#760303", "#FF0000", "#FF8000", "#FFFF00", "#009900", "#0000FF", "#BF1DBF", "#999999", "#FFFFFF")
        private val mHandler = Handler()
        private val sdf = SimpleDateFormat("H:mm:ss")
        private var mScreenFlag = true
        private var mCount = 0
        var gStarted = false

        private fun toColored(s: String): String = s.map { it -> if (it >= '0' && it <= '9') "<font color=\"${mCol[it - '0']}\">$it</font>" else it }.joinToString("")

        private fun updateWidget() {
            val date = Date(System.currentTimeMillis())
            val dStr = sdf.format(date)
            val mViews = RemoteViews(packageName, R.layout.resistor_color_code_clock_widget)
            val mThisWidget = ComponentName(this, ResistorColorCodeClockWidget::class.java)

            mViews.setTextViewText(
                    R.id.appwidget_text,
                    //Html.fromHtml(toColored("12:34:56"))
                    Html.fromHtml(toColored(dStr))
            )
            //mViews.setTextViewText(R.id.textView, "count=${mCount}")
            Log.i(TAG, "count=${mCount}")
            mCount++

            // Instruct the widget manager to update the widget
            val mManager = AppWidgetManager.getInstance(this)
            mManager.updateAppWidget(mThisWidget, mViews)
        }

        private val r = object : Runnable {
            override fun run() {
                updateWidget()
                if (mScreenFlag) {
                    val now = System.currentTimeMillis()
                    val delay = 1000 - (now % 1000)
                    mHandler.postDelayed(this, delay)
                }
            }
        }

        private fun update() {
            mHandler.post(r)
        }


        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            super.onStartCommand(intent, flags, startId)
            Log.d(TAG, "onStart")
            if (gStarted) {
                Log.d(TAG, "already started")
                return Service.START_NOT_STICKY
            }
            Log.d(TAG, "start")
            gStarted = true

            if (Build.VERSION.SDK_INT >= 26) {
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                        "channel_1",
                        getString(R.string.channel_name),
                        NotificationManager.IMPORTANCE_NONE
                )
                channel.lightColor = Color.GREEN
                channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                manager.createNotificationChannel(channel)
                val notification = NotificationCompat.Builder(this, "channel_1")
                        .setContentTitle(getString(R.string.notification_title))
                        .setContentText(getString(R.string.notification_text)).build()

                startForeground(1, notification)
            }

            val bcr = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    Log.d(TAG, "onReceive context=${context} intent=${intent}")
                    if (intent?.action.equals(Intent.ACTION_SCREEN_ON)) {
                        Log.d(TAG, "onReceive ACTION_SCREEN_ON")
                        mScreenFlag = true
                        update()
                    } else if (intent?.action.equals(Intent.ACTION_SCREEN_OFF)) {
                        Log.d(TAG, "onReceive ACTION_SCREEN_OFF")
                        mScreenFlag = false
                    }
                }
            }

            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_SCREEN_OFF)
            filter.addAction(Intent.ACTION_SCREEN_ON)
            applicationContext.registerReceiver(bcr, filter);

            update()

            return Service.START_STICKY
        }

        override fun onCreate() {
            super.onCreate()
            Log.d(TAG, "onCreate")
        }

        override fun onDestroy() {
            super.onDestroy()
            mHandler.removeCallbacks(r)
            gStarted = false
            Log.d(TAG, "onDestroy")
        }

        override fun onBind(intent: Intent): IBinder? {
            return null
        }

    }

}

