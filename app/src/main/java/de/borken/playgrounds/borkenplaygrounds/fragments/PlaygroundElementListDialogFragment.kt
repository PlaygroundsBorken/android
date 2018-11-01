package de.borken.playgrounds.borkenplaygrounds.fragments

import android.content.Context
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import de.borken.playgrounds.borkenplaygrounds.R
import de.borken.playgrounds.borkenplaygrounds.dp
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import de.borken.playgrounds.borkenplaygrounds.models.tryParsePlaygroundElements
import kotlinx.android.synthetic.main.fragment_playgroundelement_list_dialog.*
import kotlinx.android.synthetic.main.fragment_playgroundelement_list_dialog_item.view.*
import java.io.Serializable

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

    private fun loadPlaygroundElements() {
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
                    list.adapter = PlaygroundElementAdapter(playgroundElements.size)
                    list.layoutManager = GridLayoutManager(context, 3)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val playgroundList = arguments?.getSerializable("playground_element") as? List<*>

        playgroundElements = playgroundList?.filter { it is PlaygroundElement }?.map {
            it as PlaygroundElement
        }.orEmpty()


        if (playgroundElements.isEmpty())
            loadPlaygroundElements()
        else {
            list.adapter = PlaygroundElementAdapter(playgroundElements.size)
            list.layoutManager = GridLayoutManager(context, 3)
        }
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
        mListener?.onSelectedPlaygroundElements(playgroundElements)
        mListener = null
        super.onDetach()
    }

    interface Listener {
        fun onSelectedPlaygroundElements(elements: List<PlaygroundElement>)
    }

    private fun togglePlaygroundElement(playgroundElement: PlaygroundElement, playgroundElementImageView: ImageView) {
        var padding = 8.dp
        var backgroundResource = R.drawable.image_border
        if (playgroundElement.selected) {
            padding = 16.dp
            backgroundResource = R.drawable.image_border_selected
        }

        playgroundElementImageView.setBackgroundResource(backgroundResource)
        playgroundElementImageView.setPadding(padding, padding, padding, padding)
    }

    private inner class ViewHolder internal constructor(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.fragment_playgroundelement_list_dialog_item, parent, false)) {

        internal val playgroundElementImageView: ImageView = itemView.playgroundElement
        internal lateinit var playgroundElement: PlaygroundElement

        init {

            playgroundElementImageView.setOnClickListener {
                playgroundElement.selected = playgroundElement.selected.not()
                togglePlaygroundElement(playgroundElement, it as ImageView)
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
            val element = playgroundElements[position]

            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(element.image)

            holder.playgroundElement = element
            togglePlaygroundElement(holder.playgroundElement, holder.playgroundElementImageView)
            Glide.with(viewContext)
                .load(storageReference)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.playgroundElementImageView)
        }

        override fun getItemCount(): Int {
            return mItemCount
        }
    }

    companion object {

        fun newInstance(selectedElements: List<PlaygroundElement>): PlaygroundElementListDialogFragment =
            PlaygroundElementListDialogFragment().apply {
                arguments = Bundle().apply {
                    //val string = getString(R.string.playground_element)
                    putSerializable("playground_element", selectedElements as Serializable)
                }
            }

    }
}
