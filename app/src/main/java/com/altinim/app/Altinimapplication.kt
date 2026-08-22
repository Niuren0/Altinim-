package com.altinim.app

import android.app.Application
import com.altinim.app.data.AppContainer

class AltinimApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}