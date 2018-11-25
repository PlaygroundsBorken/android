package de.borken.playgrounds.borkenplaygrounds

import android.app.Application
import android.content.Context
import de.borken.playgrounds.borkenplaygrounds.models.User

class PlaygroundApplication : Application() {

    var activeUser: User? = null
}

val Context.playgroundApp: PlaygroundApplication
    get() = applicationContext as PlaygroundApplication