package de.borken.playgrounds.borkenplaygrounds.models

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.QuerySnapshot
import de.borken.playgrounds.borkenplaygrounds.toStringList
import java.io.Serializable

class User(
    val deviceId: String,
    val visitedPlaygrounds: Int

) : Serializable {

    interface UserCreated {

        fun userIsCreated(user: User?)
    }

    companion object {
        fun tryParse(result: QuerySnapshot?): User? {

            val users = result?.documents?.mapNotNull { documentSnapshot ->

                tryParseSingleDocument(documentSnapshot)
            }

            return if (users.isNullOrEmpty()) {
                null
            } else {
                users[0]
            }
        }

        fun createNewUser(android_ID: String, userCreated: UserCreated) {

            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            val db = FirebaseFirestore.getInstance()
            db.firestoreSettings = settings

            db.collection("users").add(
                User.newUser(
                    android_ID
                )
            ).addOnSuccessListener { documentReference ->

                documentReference.get().addOnSuccessListener {

                    val tryParseSingleDocument = User.tryParseSingleDocument(it)
                    userCreated.userIsCreated(tryParseSingleDocument)
                }.addOnFailureListener {
                    userCreated.userIsCreated(null)
                }
            }.addOnFailureListener {
                userCreated.userIsCreated(null)
            }
        }

        private fun newUser(android_ID: String): MutableMap<String, Any> {

            val user = mutableMapOf<String, Any>()

            user["deviceId"] = android_ID
            user["visitedPlaygrounds"] = 0
            user["downVotedPlaygrounds"] = emptyMap<String, Boolean>()
            user["upVotedPlaygrounds"] = emptyMap<String, Boolean>()
            user["userRemarks"] = emptyMap<String, Boolean>()

            return user
        }

        private fun tryParseSingleDocument(documentSnapshot: DocumentSnapshot): User? {

            val deviceId = documentSnapshot.getString("deviceId")
            val visitedPlaygrounds = documentSnapshot.getLong("visitedPlaygrounds")

            val downVotedPlaygrounds = (documentSnapshot.get("downVotedPlaygrounds") as? Map<*, *>).toStringList

            val upVotedPlaygrounds = (documentSnapshot.get("upVotedPlaygrounds") as? Map<*, *>).toStringList

            val userRemarks = (documentSnapshot.get("userRemarks") as? Map<*, *>).toStringList

            return if (!deviceId.isNullOrEmpty() && visitedPlaygrounds !== null)
                User(
                    deviceId,
                    visitedPlaygrounds.toInt()
                ).setLists(downVotedPlaygrounds, upVotedPlaygrounds, userRemarks)
            else
                null
        }
    }

    private var mUserRemarks: MutableList<String> = mutableListOf()

    var mUpVotedPlaygrounds: MutableList<String> = mutableListOf()

    var mDownVotedPlaygrounds: MutableList<String> = mutableListOf()

    private fun setLists(
        downVotedPlaygrounds: List<String>,
        upVotedPlaygrounds: List<String>,
        userRemarks: List<String>
    ): User {

        this.mDownVotedPlaygrounds.addAll(downVotedPlaygrounds)
        this.mUpVotedPlaygrounds.addAll(upVotedPlaygrounds)
        this.mUserRemarks.addAll(userRemarks)

        return this
    }
}
