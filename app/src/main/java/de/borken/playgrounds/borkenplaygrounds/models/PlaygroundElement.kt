package de.borken.playgrounds.borkenplaygrounds.models

import com.google.firebase.firestore.QuerySnapshot
import java.io.Serializable

class PlaygroundElement(
    val id: String,
    val name: String,
    val image: String
): Serializable


fun tryParsePlaygroundElements(result: QuerySnapshot?): List<PlaygroundElement>? {

    return result?.documents?.mapNotNull {

        val name = it.getString("name")
        val image = it.getString("image")
        val id = it.id

        if (name != null && image != null)
            PlaygroundElement(id, name, image)
        else
            null
    }
}
