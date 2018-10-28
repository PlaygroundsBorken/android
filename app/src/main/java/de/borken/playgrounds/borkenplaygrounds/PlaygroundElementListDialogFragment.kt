package de.borken.playgrounds.borkenplaygrounds

import android.content.Context
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import com.mikhaellopez.circularimageview.CircularImageView
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import de.borken.playgrounds.borkenplaygrounds.models.tryParsePlaygroundElements
import kotlinx.android.synthetic.main.activity_splash_screen.*
import kotlinx.android.synthetic.main.fragment_playgroundelement_list_dialog.*
import kotlinx.android.synthetic.main.fragment_playgroundelement_list_dialog_item.*

/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    PlaygroundElementListDialogFragment.newInstance().show(supportFragmentManager, "dialog")
 * </pre>
 *
 * You activity (or fragment) needs to implement [PlaygroundElementListDialogFragment.Listener].
 */
class PlaygroundElementListDialogFragment : BottomSheetDialogFragment() {
    private var mListener: Listener? = null
    private var playgroundElements: List<PlaygroundElement> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_playgroundelement_list_dialog, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = settings
        db.collection("playground-elements")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    this.playgroundElements = tryParsePlaygroundElements(task.result).orEmpty()
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list.layoutManager = GridLayoutManager(context, 3)
        list.adapter = PlaygroundElementAdapter(playgroundElements.size)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parent = parentFragment
        mListener = if (parent != null) {
            parent as Listener
        } else {
            context as Listener
        }
    }

    override fun onDetach() {
        mListener = null
        super.onDetach()
    }

    interface Listener {
        fun onPlaygroundElementClicked(position: Int)
    }

    private inner class ViewHolder internal constructor(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.fragment_playgroundelement_list_dialog_item, parent, false)) {

        init {
            playgroundElement.setOnClickListener {
                (it as? CircularImageView)?.setBorderWidth(4.0F)
            }
        }
    }

    private inner class PlaygroundElementAdapter internal constructor(private val mItemCount: Int) :
        RecyclerView.Adapter<ViewHolder>() {

        internal lateinit var viewContext: Context

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            viewContext = parent.context
            return ViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            if (playgroundElements.size < position) return
            val playgroundElement = playgroundElements[position]

            val storageReference = FirebaseStorage.getInstance().reference.child("badges/" + playgroundElement.image)

            Glide.with(viewContext)
                .load(storageReference)
                .into(imageView)
        }

        override fun getItemCount(): Int {
            return mItemCount
        }
    }

    companion object {

        fun newInstance(): PlaygroundElementListDialogFragment =
            PlaygroundElementListDialogFragment().apply {
                arguments = Bundle().apply {

                }
            }

    }
}
