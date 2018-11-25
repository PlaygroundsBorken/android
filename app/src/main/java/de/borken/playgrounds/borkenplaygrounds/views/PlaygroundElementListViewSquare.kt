package de.borken.playgrounds.borkenplaygrounds.views

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import de.borken.playgrounds.borkenplaygrounds.R
import de.borken.playgrounds.borkenplaygrounds.models.Playground
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import kotlinx.android.synthetic.main.fragment_playgroundelement_list_dialog_item_square.view.*


class PlaygroundElementListViewSquare : RecyclerView, Playground.PlaygroundElementsListener {
    override fun playgroundElementsLoaded(playgroundElements: List<PlaygroundElement>) {

        playgroundElements.forEach {
            playgroundElementAdded(it)
        }
    }

    private var playgroundElements: MutableList<PlaygroundElement> = mutableListOf()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun playgroundElementAdded(element: PlaygroundElement) {
        this.playgroundElements.add(element)
        adapter = PlaygroundElementSquareAdapter(playgroundElements.size)
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
    }

    private inner class ViewHolder internal constructor(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.fragment_playgroundelement_list_dialog_item_square, parent, false)) {

        internal val playgroundElementImageView: ImageView = itemView.playgroundElementSquare
        internal lateinit var playgroundElement: PlaygroundElement

        init {
        }
    }

    private inner class PlaygroundElementSquareAdapter internal constructor(private val mItemCount: Int) :
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
            Glide.with(viewContext)
                .load(element.image)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.playgroundElementImageView)
        }

        override fun getItemCount(): Int {
            return mItemCount
        }
    }
}
