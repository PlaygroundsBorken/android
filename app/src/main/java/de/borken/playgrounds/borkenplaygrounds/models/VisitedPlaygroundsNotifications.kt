package de.borken.playgrounds.borkenplaygrounds.models

import android.app.Activity
import com.tapadoo.alerter.Alerter

class VisitedPlaygroundNotification(val title: String, val text: String)

class VisitedPlaygroundsNotifications() {

    private val texts = mutableMapOf<Int, VisitedPlaygroundNotification>()

    init {
        texts[1] = VisitedPlaygroundNotification("Dein erster Spielplatz!", "Klasse Du hast deinen ersten Spielplatz besucht! Weiter so!!!")
    }


    fun showNotification(visitedPlaygrounds: Int, activity: Activity) {

        val playgroundNotification = texts[visitedPlaygrounds]

        if (playgroundNotification !== null) {
            Alerter.create(activity)
                .setTitle(playgroundNotification.title)
                .setText(playgroundNotification.text)
                .show()
        }
    }
}