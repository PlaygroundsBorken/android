package de.borken.playgrounds.borkenplaygrounds.models

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import java.io.Serializable

class PlaygroundElement(
    val id: String,
    val name: String,
    val image: String,
    var selected: Boolean = false
) : Serializable


fun tryParsePlaygroundElements(result: QuerySnapshot?): List<PlaygroundElement> {

    return result?.documents?.mapNotNull {

        tryParsePlaygroundElements(it)
    }.orEmpty()
}

fun tryParsePlaygroundElements(result: List<DocumentSnapshot>): List<PlaygroundElement> {

    return result.mapNotNull {
        tryParsePlaygroundElements(it)
    }
}

fun tryParsePlaygroundElements(result: DocumentSnapshot?): PlaygroundElement? {

    val name = result?.getString("name")
    val image = result?.getString("image")
    val id = result?.id

    return if (id != null && name != null && image != null)
        PlaygroundElement(id, name, image)
    else
        null
}
