package de.borken.playgrounds.borkenplaygrounds.models

import com.google.gson.Gson

class VisitedPlaygroundNotification(val title: String, val text: String, val visitedPlaygrounds: Int)

class VisitedPlaygroundsNotifications(val visitedPlaygroundsNotifications: List<VisitedPlaygroundNotification>) {

    companion object {

        fun fromJson(jsonObject: String): VisitedPlaygroundsNotifications {

            return Gson().fromJson(jsonObject, VisitedPlaygroundsNotifications::class.java)
        }
    }
}