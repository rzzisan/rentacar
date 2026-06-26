package com.rzzisan.carrental

import android.app.Application

class CarRentalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.init(this)
    }
}

object AppContext {
    lateinit var app: Application
    fun init(application: Application) { app = application }
}
