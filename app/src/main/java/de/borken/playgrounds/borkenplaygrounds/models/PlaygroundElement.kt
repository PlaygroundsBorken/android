package de.borken.playgrounds.borkenplaygrounds.models

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import java.io.Serializable

class PlaygroundElement(
    val id: String,
    val name: String,
    val image: String,
    var selected: Boolean = false
) : Serializable, Comparable<PlaygroundElement> {

    override fun compareTo(other: PlaygroundElement): Int {
        return if (this.id == other.id) {
            0
        } else {
            -1
        }
    }

    override fun hashCode(): Int {

        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaygroundElement

        if (id != other.id) return false

        return true
    }
}


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
