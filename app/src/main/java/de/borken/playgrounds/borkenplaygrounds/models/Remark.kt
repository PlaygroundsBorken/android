package de.borken.playgrounds.borkenplaygrounds.models

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.QuerySnapshot
import java.io.Serializable

class Remark(
    val text: String
) : Serializable {

    companion object {

        private fun tryParseRemarks(documents: QuerySnapshot?): List<Remark> {

            val remarks = documents?.documents?.mapNotNull { documentSnapshot ->

                val text = documentSnapshot.getString("text")
                val remarkee = documentSnapshot.getString("remarkee")
                val remarkedPlayground = documentSnapshot.getString("remarkedPlayground")

                if (text != null && remarkee != null && remarkedPlayground != null)
                    Remark(text).setLists(remarkee, remarkedPlayground)
                else
                    null
            }

            return remarks.orEmpty()
        }

        fun newRemark(documentId: String?, id: String?, toString: String): MutableMap<String, Any>? {

            if (documentId == null || id == null) {
                return null
            }

            val remark = mutableMapOf<String, Any>()
            remark["__typename"] = "UserRemark"
            remark["remarkedPlayground"] = id
            remark["remarkee"] = documentId
            remark["text"] = toString

            return remark
        }
    }

    private lateinit var mRemarkee: String

    private lateinit var mRemarkedPlayground: String

    private fun setLists(remarkee: String, remarkedPlayground: String): Remark {

        this.mRemarkee = remarkee
        this.mRemarkedPlayground = remarkedPlayground
        return this
    }

}