package de.borken.playgrounds.borkenplaygrounds.models

import com.google.firebase.firestore.QuerySnapshot
import com.mapbox.geojson.Point
import java.io.Serializable

class Playground(
    val id: String,
    val name: String,
    val location: Point,
    val description: String? = "",
    val age: String? = "",
    val rating: Long? = 0
): Serializable


fun tryParsePlaygrounds(result: QuerySnapshot?): List<Playground>? {

    return result?.documents?.mapNotNull {

        val name = it.getString("name")
        val location = it.getGeoPoint("position")
        val description = it.getString("description")
        val age = it.getString("age")
        val rating = it.getLong("rating")
        val id = it.id

        if (name != null && location != null)
            Playground(id, name, Point.fromLngLat(location.longitude, location.latitude), description, age, rating)
        else
            null
    }
}
