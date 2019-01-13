package de.borken.playgrounds.borkenplaygrounds.fragments

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.text.Html
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import com.glide.slider.library.SliderLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import de.borken.playgrounds.borkenplaygrounds.R
import de.borken.playgrounds.borkenplaygrounds.animation.ZoomAnimator
import de.borken.playgrounds.borkenplaygrounds.glide.PlaygroundSliderView
import de.borken.playgrounds.borkenplaygrounds.models.Playground
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import de.borken.playgrounds.borkenplaygrounds.playgroundApp
import kotlinx.android.synthetic.main.fragment_playground_list_dialog.*


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
class PlaygroundListDialogFragment : BottomSheetDialogFragment(), Playground.PlaygroundElementsListener {

    override fun playgroundElementsLoaded(playgroundElements: List<PlaygroundElement>) {

        playgroundElements.forEach {
            playground_elements.playgroundElementAdded(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Retrieve and cache the system's default "short" animation time.

        return inflater.inflate(R.layout.fragment_playground_list_dialog, container, false)
    }

    private var playground: Playground? = null

    private var fusedLocationClient: FusedLocationProviderClient? = null

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        this.playground = arguments?.getSerializable("PLAYGROUND_LIST") as? Playground

        playground_name.text = playground?.name

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            playground_description.text =
                    Html.fromHtml(playground?.description.orEmpty(), Html.FROM_HTML_MODE_COMPACT)
        } else {
            playground_description.text = Html.fromHtml(playground?.description.orEmpty())
        }

        if (playground?.rating?.toFloat() !== null)
            playground_rating.rating = playground!!.rating!!.toFloat()
        playground?.images.orEmpty().forEach { image ->

            val sliderView = PlaygroundSliderView(view.context)
            sliderView
                .image(image)
                .setBackgroundColor(Color.WHITE)
                .setProgressBarVisible(true)

            sliderView.setOnSliderClickListener {
                it as PlaygroundSliderView
                ZoomAnimator(this.context!!).zoomImageFromThumb(expanded_image, it.imageView, image, container, list_item_container)
            }

            playground_images_slider.addSlider(sliderView)
        }

        playground_images_slider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom)
        playground_images_slider.setDuration(10000)

        playground?.loadPlaygroundElements(this)
        val upVotedPlaygrounds = context?.applicationContext?.playgroundApp?.activeUser?.mUpVotedPlaygrounds
        val downVotedPlaygrounds = context?.applicationContext?.playgroundApp?.activeUser?.mDownVotedPlaygrounds

        upvote.setOnClickListener {
            voteClickHandler(upVotedPlaygrounds, downVotedPlaygrounds, it, true)
        }

        downvote.setOnClickListener {
            voteClickHandler(upVotedPlaygrounds, downVotedPlaygrounds, it, false)
        }

        playground_remarks.setOnClickListener {
            remarksClickHandler(view)
        }

        //val mBehavior = BottomSheetBehavior.from(list)
        if (activity != null)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)

        if (fusedLocationClient != null) {

            fusedLocationClient!!.lastLocation
                .addOnSuccessListener { location : Location? ->

                    val playgroundLocation = Location("point B")
                    playgroundLocation.latitude = playground?.location?.latitude() ?: 0.0
                    playgroundLocation.longitude = playground?.location?.longitude() ?: 0.0

                    val distance = location?.distanceTo(playgroundLocation)

                    if (distance !== null && distance < 100) {

                        playground_button_container.visibility = VISIBLE
                    }
                }
        }
    }

    private fun voteClickHandler(
        upVotedPlaygrounds: MutableList<String>?,
        downVotedPlaygrounds: MutableList<String>?,
        it: View?,
        isUpVote: Boolean
    ) {
        val playgroundIsUpVoted = upVotedPlaygrounds?.contains(playground?.id) ?: false
        val playgroundIsDownVoted = downVotedPlaygrounds?.contains(playground?.id) ?: false
        val from = FloatArray(3)
        val to = FloatArray(3)

        Color.colorToHSV(Color.parseColor("#FF707070"), from)
        Color.colorToHSV(Color.parseColor("#FF42f44b"), to)

        fun savePlaygrounds() {
            if (isUpVote) {
                upVotedPlaygrounds?.add(playground!!.id)
            } else {
                downVotedPlaygrounds?.add(playground!!.id)
            }
        }

        if (!playgroundIsUpVoted && !playgroundIsDownVoted) {

            if (playgroundIsDownVoted && !isUpVote || playgroundIsUpVoted && isUpVote) {
                animateImageButton(to, from, downvote)
                downvote.scaleType = ImageView.ScaleType.CENTER
                if (playground != null) {
                    if (isUpVote) {
                        downVotedPlaygrounds?.add(playground!!.id)
                    } else {
                        upVotedPlaygrounds?.add(playground!!.id)
                    }
                }
            }

            animateImageButton(from, to, it as ImageButton)
            it.scaleType = ImageView.ScaleType.CENTER_CROP
            if (playground != null) {
                savePlaygrounds()
            }
        } else {

            animateImageButton(to, from, it as ImageButton)
            it.scaleType = ImageView.ScaleType.CENTER
            if (playground != null) {
                savePlaygrounds()
            }
        }
    }

    private fun remarksClickHandler(view: View) {
        val builder = AlertDialog.Builder(view.context)
        builder.setTitle("Title")

        val input = EditText(view.context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton(
            "OK"
        ) { _, _ ->
            saveCustomerRemark(input.text.toString(), playground)

        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun animateImageButton(from: FloatArray, to: FloatArray, imageButton: ImageButton) {

        val anim = ValueAnimator.ofFloat(0.0f, 1.0f)
        anim.duration = 1000

        val hsv = FloatArray(3)

        anim.addUpdateListener {
            for (index in 0.rangeTo(2)) {
                hsv[index] = from[index] + (to[index] - from[index]) * it.animatedFraction
            }

            imageButton.setColorFilter(Color.HSVToColor(hsv))
        }

        anim.start()

    }


    private fun saveCustomerRemark(toString: String, playground: Playground?) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
