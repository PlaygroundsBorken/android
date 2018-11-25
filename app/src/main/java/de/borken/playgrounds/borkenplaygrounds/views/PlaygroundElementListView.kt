package de.borken.playgrounds.borkenplaygrounds.views

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import de.borken.playgrounds.borkenplaygrounds.R
import de.borken.playgrounds.borkenplaygrounds.dp
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import de.borken.playgrounds.borkenplaygrounds.models.tryParsePlaygroundElements
import kotlinx.android.synthetic.main.fragment_playgroundelement_list_dialog_item.view.*


class PlaygroundElementListView : RecyclerView {


    private var _elementsAreClickable: Boolean = false
    private var _elementsPerRow: Int = 2
    private var _overflowHorizontal: Boolean = false
    private var playgroundElements: MutableList<PlaygroundElement> = mutableListOf()
    private var selectedPlaygroundElements: List<PlaygroundElement> = mutableListOf()

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    private var listener: Listener? = null

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.PlaygroundElementListView, defStyle, 0
        )

        _elementsAreClickable = a.getBoolean(
            R.styleable.PlaygroundElementListView_elementsAreSelectable,
            false
        )

        _elementsPerRow = a.getInt(
            R.styleable.PlaygroundElementListView_elementsPerRow,
            2
        )

        _overflowHorizontal = a.getBoolean(
            R.styleable.PlaygroundElementListView_overFlowHorizontal,
            false
        )

        a.recycle()
    }


    fun setup(playgroundElements: List<PlaygroundElement>?, listener: Listener? = null) {

        this.listener = listener
        this.selectedPlaygroundElements = playgroundElements.orEmpty()
        loadPlaygroundElements()
    }

    private fun loadPlaygroundElements() {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = settings
        db.collection("playgroundelements")
            .whereEqualTo("trash", false)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    this.playgroundElements = tryParsePlaygroundElements(task.result).toMutableList()
                    adapter = PlaygroundElementAdapter()
                    layoutManager = if (_overflowHorizontal)
                        LinearLayoutManager(context, HORIZONTAL, false)
                    else
                        GridLayoutManager(context, _elementsPerRow)
                }
            }
    }

    private fun togglePlaygroundElement(playgroundElement: PlaygroundElement, playgroundElementImageView: ImageView) {
        val padding = 8.dp
        var backgroundResource = R.drawable.image_border
        if (playgroundElement.selected) {
            //padding = 16.dp
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

            if (_elementsAreClickable) {
                playgroundElementImageView.setOnClickListener {
                    playgroundElement.selected = playgroundElement.selected.not()
                    togglePlaygroundElement(playgroundElement, it as ImageView)

                    if (playgroundElement.selected)
                        listener?.playgroundElementSelected(playgroundElement)
                    else
                        listener?.playgroundElementDeselected(playgroundElement)
                }
            }
        }
    }

    private inner class PlaygroundElementAdapter internal constructor() :
        RecyclerView.Adapter<ViewHolder>() {

        internal lateinit var viewContext: Context

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            viewContext = parent.context
            return ViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            if (playgroundElements.size < position) return
            val element = playgroundElements[position]

            holder.playgroundElement = element
            togglePlaygroundElement(holder.playgroundElement, holder.playgroundElementImageView)
            Glide.with(viewContext)
                .load(element.image)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.playgroundElementImageView)

            if (selectedPlaygroundElements.any { it.id == element.id }) {
                element.selected = true
                togglePlaygroundElement(element, holder.playgroundElementImageView)
            }
        }

        override fun getItemCount(): Int {
            return playgroundElements.size
        }
    }

    interface Listener {
        fun playgroundElementSelected(element: PlaygroundElement)
        fun playgroundElementDeselected(element: PlaygroundElement)
    }
}
