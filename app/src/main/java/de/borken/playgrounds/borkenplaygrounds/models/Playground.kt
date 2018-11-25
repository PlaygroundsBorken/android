package de.borken.playgrounds.borkenplaygrounds.models

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
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
    val images: List<String> = emptyList(),
    val upVotes: Int = 0,
    val downVotes: Int = 0
) : Serializable {

    fun loadPlaygroundElements(listener: PlaygroundElementsListener) {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = settings

        db.collection("playgroundelements")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val list = querySnapshot.documents.filter { it.id in mPlaygroundElements }
                val playgroundElements = tryParsePlaygroundElements(list)
                listener.playgroundElementsLoaded(playgroundElements)
            }
    }

    interface PlaygroundElementsListener {
        fun playgroundElementsLoaded(playgroundElements: List<PlaygroundElement>)
    }

    companion object {

        fun tryParsePlaygrounds(result: QuerySnapshot?): List<Playground>? {

            return result?.documents?.mapNotNull { documentSnapshot ->

                val name = documentSnapshot.getString("name")
                val lat = documentSnapshot.getString("lat")
                val lng = documentSnapshot.getString("lng")
                val description = documentSnapshot.get("description") as Map<*, *>
                val age = documentSnapshot.getString("age")
                val rating = documentSnapshot.getLong("rating")
                val ratingCount = documentSnapshot.getLong("ratingCount")
                val id = documentSnapshot.id
                val images = documentSnapshot.get("images") as? ArrayList<*>
                val playgroundElements = documentSnapshot.get("items") as? Map<*, *>
                val downVotes = documentSnapshot.getLong("downVotes")?.toInt()?.or(0)!!
                val upVotes = documentSnapshot.getLong("upVotes")?.toInt()?.or(0)!!

                val imageList = images?.filter {
                    it is Map<*, *>
                            && it.containsKey("image")
                            && it["image"] is Map<*, *>
                            && (it["image"] as Map<*, *>).containsKey("url")
                }?.mapNotNull {
                    (((it as Map<*, *>)["image"]) as Map<*, *>)["url"] as? String
                }.orEmpty()

                val playgroundElementList =
                    playgroundElements?.filter { it.value is Boolean && it.key is String && it.value as Boolean }
                        ?.map { it.key as String }.orEmpty()

                if (name != null
                    && lat != null && lat.toDoubleOrNull() != null
                    && lng != null && lng.toDoubleOrNull() != null
                ) {

                    var playgroundHtml: String? = ""
                    if (description.containsKey("html")) {

                        playgroundHtml = description["html"] as? String
                    }

                    Playground(
                        id,
                        name,
                        Point.fromLngLat(lng.toDouble(), lat.toDouble()),
                        playgroundHtml.orEmpty(),
                        age,
                        rating,
                        ratingCount,
                        imageList,
                        upVotes,
                        downVotes
                    ).setList(playgroundElementList)
                } else
                    null
            }
        }
    }

    lateinit var mPlaygroundElements: List<String>

    private fun setList(playgroundElements: List<String>): Playground {

        this.mPlaygroundElements = playgroundElements
        return this
    }
}



