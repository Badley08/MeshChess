package com.karlitodev.meshchess

import android.app.Application

class MeshChess : Application() {
    override fun onCreate() {
        super.onCreate()

        // Chaquopy is initialized lazily via the generated PythonProvider.
        // No manual Python.start() call is needed with Chaquopy 15+.
        // The plugin auto-generates a ContentProvider that handles it.
    }
}
