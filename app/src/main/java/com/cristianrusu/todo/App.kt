package com.cristianrusu.todo

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)?.let {
                Log.i("Firebase-init", "Firebase initialized in Application class.")
            } ?: run {
                Log.e("Firebase-init", "FirebaseApp is null in Application class.")
            }
        } catch (e: Exception) {
            Log.e("Firebase-init", "Exception during Firebase initialization: ${e.message}", e)
        }
    }
}