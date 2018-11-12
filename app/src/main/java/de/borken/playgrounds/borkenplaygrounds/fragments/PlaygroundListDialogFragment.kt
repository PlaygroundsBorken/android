package de.borken.playgrounds.borkenplaygrounds.fragments

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.glide.slider.library.SliderLayout
import com.glide.slider.library.SliderTypes.DefaultSliderView
import de.borken.playgrounds.borkenplaygrounds.R
import de.borken.playgrounds.borkenplaygrounds.models.Playground
import kotlinx.android.synthetic.main.fragment_playground_list_dialog.*
import kotlinx.android.synthetic.main.fragment_playground_list_dialog_item.view.*


/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    PlaygroundListDialogFragment.newInstance(playground).show(supportFragmentManager, "dialog")
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
        list.adapter =
                PlaygroundAdapter(arguments?.getSerializable("PLAYGROUND_LIST") as? Playground)
    }

    private inner class ViewHolder internal constructor(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.fragment_playground_list_dialog_item, parent, false)) {

        internal val text: TextView = itemView.playground_name
        internal val playgroundImageSlider = itemView.playground_images_slider
        internal val playgroundDescription = itemView.playground_description
        internal val playgroundRating = itemView.playground_rating
        internal val playgroundElements = itemView.playground_elements

        init {

        }
    }

    private inner class PlaygroundAdapter internal constructor(private val mPlayground: Playground?) :
        RecyclerView.Adapter<ViewHolder>() {

        override fun getItemCount(): Int = 1

        private lateinit var viewContext: Context

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            viewContext = parent.context
            return ViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            holder.text.text = mPlayground?.name

            var descriptionText = "<p>" + mPlayground?.description + "</p><br>"
            mPlayground?.bulletPoints?.forEach {
                descriptionText += "<li>"
                descriptionText += " "
                descriptionText += it
                descriptionText += "</li>"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                holder.playgroundDescription.text = Html.fromHtml(descriptionText, Html.FROM_HTML_MODE_COMPACT)
            } else {
                holder.playgroundDescription.text = Html.fromHtml(descriptionText)
            }

            if (mPlayground?.rating?.toFloat() !== null)
                holder.playgroundRating.rating = mPlayground.rating.toFloat()
            mPlayground?.images.orEmpty().forEach {

                val sliderView = DefaultSliderView(viewContext)
                sliderView
                    .image(it)
                    .setBackgroundColor(Color.WHITE)
                    .setProgressBarVisible(true)

                holder.playgroundImageSlider.addSlider(sliderView)
            }

            holder.playgroundImageSlider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom)
            holder.playgroundImageSlider.setDuration(4000)

            holder.playgroundElements.setup(mPlayground?.playgroundElements.orEmpty())
        }
    }

    companion object {

        fun newInstance(playgroundId: Playground?): PlaygroundListDialogFragment =

            PlaygroundListDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("PLAYGROUND_LIST", playgroundId)
                }
            }
    }
}
