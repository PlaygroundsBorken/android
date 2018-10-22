package de.borken.playgrounds.borkenplaygrounds.models

import com.google.firebase.firestore.QuerySnapshot
import com.mapbox.geojson.Point

class Playground(val name: String, val location: Point) {

    companion object {
        val NAME_IDENTIFIER: String = "PLAYGROUND_NAME"
    }
}


fun tryParsePlaygrounds(result: QuerySnapshot?): List<Playground>? {

    return result?.documents?.mapNotNull {

        val name = it.getString("name")
        val location = it.getGeoPoint("position")

        if (name != null && location != null)
            Playground(name, Point.fromLngLat(location.longitude, location.latitude))
        else
            null
    }
}
