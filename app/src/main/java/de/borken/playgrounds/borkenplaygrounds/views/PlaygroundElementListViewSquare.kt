package de.borken.playgrounds.borkenplaygrounds.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import de.borken.playgrounds.borkenplaygrounds.databinding.FragmentPlaygroundelementListDialogItemSquareBinding
import de.borken.playgrounds.borkenplaygrounds.models.Playground
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement


class PlaygroundElementListViewSquare : RecyclerView, Playground.PlaygroundElementsListener {
    override fun playgroundElementsLoaded(playgroundElements: List<PlaygroundElement>) {

        playgroundElements.forEach {
            playgroundElementAdded(it)
        }
    }

    private var playgroundElements: MutableList<PlaygroundElement> = mutableListOf()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    fun playgroundElementAdded(element: PlaygroundElement) {
        this.playgroundElements.add(element)
        adapter = PlaygroundElementSquareAdapter(playgroundElements.size)
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
    }

    private inner class ViewHolder(binding: FragmentPlaygroundelementListDialogItemSquareBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val playgroundElementImageView: ImageView = binding.playgroundElementSquare
        lateinit var playgroundElement: PlaygroundElement

    }

    private inner class PlaygroundElementSquareAdapter(private val mItemCount: Int) :
        RecyclerView.Adapter<ViewHolder>() {

        lateinit var viewContext: Context

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            viewContext = parent.context
            val layoutInflater = LayoutInflater.from(parent.context)
            val itemBinding: FragmentPlaygroundelementListDialogItemSquareBinding =
                FragmentPlaygroundelementListDialogItemSquareBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )

            return ViewHolder(itemBinding)
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
