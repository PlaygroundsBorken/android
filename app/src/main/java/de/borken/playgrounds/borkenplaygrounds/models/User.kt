package de.borken.playgrounds.borkenplaygrounds.models

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.QuerySnapshot
import de.borken.playgrounds.borkenplaygrounds.toStringList
import java.io.Serializable

class User(
    val documentId: String,
    val deviceId: String

) : Serializable {

    interface UserCreated {

        fun userIsCreated(user: User?)
    }

    fun update() {

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = settings

        val document = db.collection("users").document(this.documentId)

        document.update("visitedPlaygrounds", this.mVisitedPlaygrounds.map { mapOf(Pair(it, true)) })
        document.update("downVotedPlaygrounds", this.mDownVotedPlaygrounds.map { mapOf(Pair(it, true)) })
        document.update("upVotedPlaygrounds", this.mUpVotedPlaygrounds.map { mapOf(Pair(it, true)) })
        document.update("userRemarks", this.mUserRemarks)
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
            user["visitedPlaygrounds"] = emptyMap<String, Boolean>()
            user["downVotedPlaygrounds"] = emptyMap<String, Boolean>()
            user["upVotedPlaygrounds"] = emptyMap<String, Boolean>()
            user["userRemarks"] = emptyMap<String, Boolean>()

            return user
        }

        private fun tryParseSingleDocument(documentSnapshot: DocumentSnapshot): User? {


            val deviceId = documentSnapshot.getString("deviceId")
            val visitedPlaygrounds = (documentSnapshot.get("visitedPlaygrounds") as? ArrayList<*>)?.flatMap { (it as? Map<*,*>).toStringList }.orEmpty()
            val downVotedPlaygrounds = (documentSnapshot.get("downVotedPlaygrounds") as? ArrayList<*>)?.flatMap { (it as? Map<*,*>).toStringList }.orEmpty()
            val upVotedPlaygrounds = (documentSnapshot.get("upVotedPlaygrounds") as? ArrayList<*>)?.flatMap { (it as? Map<*,*>).toStringList }.orEmpty()
            val userRemarks = (documentSnapshot.get("userRemarks") as? Map<*, *>).toStringList

            return if (!deviceId.isNullOrEmpty())
                User(
                    documentSnapshot.id,
                    deviceId
                ).setLists(downVotedPlaygrounds, upVotedPlaygrounds, userRemarks, visitedPlaygrounds)
            else
                null
        }
    }

    var mUserRemarks: MutableList<String> = mutableListOf()

    var mUpVotedPlaygrounds: MutableList<String> = mutableListOf()

    var mDownVotedPlaygrounds: MutableList<String> = mutableListOf()

    var mVisitedPlaygrounds: MutableList<String> = mutableListOf()

    private fun setLists(
        downVotedPlaygrounds: List<String>,
        upVotedPlaygrounds: List<String>,
        userRemarks: List<String>,
        visitedPlaygrounds: List<String>
    ): User {

        this.mDownVotedPlaygrounds.addAll(downVotedPlaygrounds)
        this.mUpVotedPlaygrounds.addAll(upVotedPlaygrounds)
        this.mUserRemarks.addAll(userRemarks)
        this.mVisitedPlaygrounds.addAll(visitedPlaygrounds)

        return this
    }
}
