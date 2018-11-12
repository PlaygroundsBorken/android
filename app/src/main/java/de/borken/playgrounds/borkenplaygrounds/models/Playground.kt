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
    val rating: Long? = 0,
    val ratingCount: Long? = 0,
    val bulletPoints: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val playgroundElements: List<PlaygroundElement> = emptyList()
): Serializable


fun tryParsePlaygrounds(result: QuerySnapshot?): List<Playground>? {

    return result?.documents?.mapNotNull { documentSnapshot ->

        val name = documentSnapshot.getString("name")
        val location = documentSnapshot.getGeoPoint("position")
        val description = documentSnapshot.getString("description")
        val age = documentSnapshot.getString("age")
        val rating = documentSnapshot.getLong("rating")
        val ratingCount = documentSnapshot.getLong("ratingCount")
        val id = documentSnapshot.id
        val images = documentSnapshot.get("images") as? List<*>
        val bulletpoints= documentSnapshot.get("bulletpoints") as? List<*>

        val playgroundElements = documentSnapshot.get("items") as? List<*>



        if (name != null && location != null)
            Playground(id, name, Point.fromLngLat(location.longitude, location.latitude), description, age, rating, ratingCount,
                bulletpoints?.filter { it is String }.orEmpty().map { it as String }, images?.filter { it is String }.orEmpty().map { it as String })
        else
            null
    }
}
