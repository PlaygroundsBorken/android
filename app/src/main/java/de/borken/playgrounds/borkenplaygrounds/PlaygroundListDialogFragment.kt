package de.borken.playgrounds.borkenplaygrounds

import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import de.borken.playgrounds.borkenplaygrounds.models.Playground
import de.borken.playgrounds.borkenplaygrounds.models.tryParsePlaygrounds
import kotlinx.android.synthetic.main.fragment_playground_list_dialog.*
import kotlinx.android.synthetic.main.fragment_playground_list_dialog_item.view.*

/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    PlaygroundListDialogFragment.newInstance(30).show(supportFragmentManager, "dialog")
 * </pre>
 *
 */
class PlaygroundListDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_playground_list_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = PlaygroundAdapter(arguments?.getString(Playground.NAME_IDENTIFIER)!!)
    }

    private inner class ViewHolder internal constructor(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.fragment_playground_list_dialog_item, parent, false)) {

        internal val text: TextView = itemView.text

        init {

        }
    }

    private inner class PlaygroundAdapter internal constructor(private val mPlaygroundName: String) :
        RecyclerView.Adapter<ViewHolder>() {

        override fun getItemCount(): Int = 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            val db = FirebaseFirestore.getInstance()
            db.firestoreSettings = settings
            db.collection("playgrounds")
                .whereEqualTo("id", mPlaygroundName)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        val tryParsePlaygrounds = tryParsePlaygrounds(task.result)

                        if (tryParsePlaygrounds !== null && tryParsePlaygrounds.isNotEmpty()) {

                            val playground = tryParsePlaygrounds.first()

                            holder.text.text = playground.name
                        }
                    }
                }
        }
    }

    companion object {

        fun newInstance(playgroundId: String): PlaygroundListDialogFragment =
            PlaygroundListDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(Playground.NAME_IDENTIFIER, playgroundId)
                }
            }

    }
}
