package de.borken.playgrounds.borkenplaygrounds

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import de.borken.playgrounds.borkenplaygrounds.models.AvatarSettings
import de.borken.playgrounds.borkenplaygrounds.models.User
import de.borken.playgrounds.borkenplaygrounds.models.VisitedPlaygroundsNotifications

class PlaygroundApplication : MultiDexApplication() {

    var activeUser: User? = null

    lateinit var remoteConfig: FirebaseRemoteConfig

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        remoteConfig = FirebaseRemoteConfig.getInstance()

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate()
    }

    fun fetchByString(name: String): String {

        return this.remoteConfig.getString(name)
    }

    companion object {

        // Remote Config keys
        const val MAPBOX_ACCESS_TOKEN = "access_token"
        const val MAPBOX_URL = "MAPBOX_STYLE"
        const val avatar_settings = "avatar_settings"
        const val rules = "rules"
        const val playground_notifications = "playground_notifications"
    }
}

val Context.playgroundApp: PlaygroundApplication
    get() = applicationContext as PlaygroundApplication

val Context.getRemoteConfig: FirebaseRemoteConfig
    get() = playgroundApp.remoteConfig

val Context.fetchMapboxAccessToken: String
    get() = getRemoteConfig.getString(PlaygroundApplication.MAPBOX_ACCESS_TOKEN)

val Context.fetchMapboxUrl: String
    get() = getRemoteConfig.getString(PlaygroundApplication.MAPBOX_URL)

val Context.fetchRules: String
    get() = getRemoteConfig.getString(PlaygroundApplication.rules)

val Context.fetchAvatarSettings: AvatarSettings
    get() = AvatarSettings.fromJson(getRemoteConfig.getString(PlaygroundApplication.avatar_settings))

val Context.fetchPlaygroundNotifications: VisitedPlaygroundsNotifications
    get() = VisitedPlaygroundsNotifications.fromJson(getRemoteConfig.getString(PlaygroundApplication.playground_notifications))