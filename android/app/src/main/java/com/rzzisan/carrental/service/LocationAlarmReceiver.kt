package com.rzzisan.carrental.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AlarmManager-এ registered BroadcastReceiver।
 * প্রতি ৫ মিনিটে OS জাগায়; LocationTrackingService বন্ধ থাকলে পুনরায় চালু করে।
 * Battery optimizer বা recents swipe-এ service মরে গেলে এটি পুনর্জীবিত করে।
 */
class LocationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rentalId = context
            .getSharedPreferences(LocationTrackingService.PREF_NAME, Context.MODE_PRIVATE)
            .getInt(LocationTrackingService.PREF_RENTAL_ID, 0)

        if (rentalId == 0) {
            // কোনো active trip নেই — alarm বাতিল করো
            LocationTrackingService.cancelAlarm(context)
            return
        }

        if (!LocationTrackingService.isRunning) {
            LocationTrackingService.start(context, rentalId)
        }
    }
}
