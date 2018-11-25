package de.borken.playgrounds.borkenplaygrounds.models

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.QuerySnapshot
import java.io.Serializable

class User(
    val deviceId: String,
    val visitedPlaygrounds: Int

) : Serializable {

    val downVotedPlaygrounds: List<Playground>
        get() {
            return getVotedPlaygrounds(this.mDownVotedPlaygrounds)

        }

    private fun getVotedPlaygrounds(downVotedPlaygrounds: List<String>): List<Playground> {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = settings
        val documents = db.collection("playgrounds")
            .whereArrayContains("id", downVotedPlaygrounds)
            .get()
            .result

        return Playground.tryParsePlaygrounds(documents).orEmpty()
    }

    val upVotedPlaygrounds: List<Playground>
        get() {
            return getVotedPlaygrounds(this.mUpVotedPlaygrounds)
        }

    val userRemarks: List<Remark>
        get() {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            val db = FirebaseFirestore.getInstance()
            db.firestoreSettings = settings
            val documents = db.collection("userRemarks")
                .whereArrayContains("id", this.mUserRemarks)
                .get()
                .result

            return Remark.tryParseRemarks(documents)
        }

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

        fun createNewUser(androiD_ID: String, userCreated: UserCreated) {

            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            val db = FirebaseFirestore.getInstance()
            db.firestoreSettings = settings

            db.collection("users").add(
                User.newUser(
                    androiD_ID
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

        private fun newUser(androiD_ID: String): MutableMap<String, Any> {

            val user = mutableMapOf<String, Any>()

            user["deviceId"] = androiD_ID
            user["visitedPlaygrounds"] = 0
            user["downVotedPlaygrounds"] = emptyMap<String, Boolean>()
            user["upVotedPlaygrounds"] = emptyMap<String, Boolean>()
            user["userRemarks"] = emptyMap<String, Boolean>()

            return user
        }

        private fun tryParseSingleDocument(documentSnapshot: DocumentSnapshot): User? {

            val deviceId = documentSnapshot.getString("deviceId")
            val visitedPlaygrounds = documentSnapshot.getLong("visitedPlaygrounds")

            val _downVotedPlaygrounds = documentSnapshot.get("downVotedPlaygrounds") as? Map<*, *>

            val _upVotedPlaygrounds = documentSnapshot.get("upVotedPlaygrounds") as? Map<*, *>

            val _userRemarks = documentSnapshot.get("userRemarks") as? Map<*, *>


            val downVotedPlaygrounds =
                _downVotedPlaygrounds?.filter { it.value is Boolean && it.key is String && it.value as Boolean }
                    .orEmpty()
                    .map { it.key as String }
            val upVotedPlaygrounds =
                _upVotedPlaygrounds?.filter { it.value is Boolean && it.key is String && it.value as Boolean }
                    .orEmpty()
                    .map { it.key as String }
            val userRemarks =
                _userRemarks?.filter { it.value is Boolean && it.key is String && it.value as Boolean }
                    .orEmpty()
                    .map { it.key as String }

            return if (!deviceId.isNullOrEmpty() && visitedPlaygrounds !== null)
                User(
                    deviceId,
                    visitedPlaygrounds.toInt()
                ).setLists(downVotedPlaygrounds, upVotedPlaygrounds, userRemarks)
            else
                null
        }
    }

    private lateinit var mUserRemarks: List<String>

    private lateinit var mUpVotedPlaygrounds: List<String>

    private lateinit var mDownVotedPlaygrounds: List<String>

    private fun setLists(
        downVotedPlaygrounds: List<String>,
        upVotedPlaygrounds: List<String>,
        userRemarks: List<String>
    ): User {

        this.mDownVotedPlaygrounds = downVotedPlaygrounds
        this.mUpVotedPlaygrounds = upVotedPlaygrounds
        this.mUserRemarks = userRemarks

        return this
    }
}
