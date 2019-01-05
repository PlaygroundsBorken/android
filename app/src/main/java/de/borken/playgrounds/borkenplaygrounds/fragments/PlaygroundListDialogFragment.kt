package de.borken.playgrounds.borkenplaygrounds.fragments

import android.animation.*
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
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
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.glide.slider.library.SliderLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import de.borken.playgrounds.borkenplaygrounds.R
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
        mShortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
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
                zoomImageFromThumb(it.imageView, image)
            }

            playground_images_slider.addSlider(sliderView)
        }

        playground_images_slider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom)
        playground_images_slider.setDuration(10000)

        //upvote.text = playground?.upVotes.toString()
        //downvote.text = playground?.downVotes.toString()

        playground?.loadPlaygroundElements(this)
        val upVotedPlaygrounds = context?.applicationContext?.playgroundApp?.activeUser?.mUpVotedPlaygrounds
        val downVotedPlaygrounds = context?.applicationContext?.playgroundApp?.activeUser?.mDownVotedPlaygrounds

        upvote.setOnClickListener {


            val playgroundIsUpVoted = upVotedPlaygrounds?.contains(playground?.id) ?: false
            val playgroundIsDownVoted = downVotedPlaygrounds?.contains(playground?.id) ?: false
            val from = FloatArray(3)
            val to = FloatArray(3)

            Color.colorToHSV(Color.parseColor("#FF707070"), from)
            Color.colorToHSV(Color.parseColor("#FF42f44b"), to)

            if (!playgroundIsUpVoted && !playgroundIsDownVoted) {

                if (playgroundIsDownVoted) {
                    animateImageButton(to, from, downvote)
                    downvote.scaleType = ImageView.ScaleType.CENTER
                    if (playground != null) {
                        downVotedPlaygrounds?.add(playground!!.id)
                    }
                }

                animateImageButton(from, to, it as ImageButton)
                it.scaleType = ImageView.ScaleType.CENTER_CROP
                if (playground != null) {
                    upVotedPlaygrounds?.add(playground!!.id)
                }
            } else {

                animateImageButton(to, from, it as ImageButton)
                it.scaleType = ImageView.ScaleType.CENTER
                if (playground != null) {
                    upVotedPlaygrounds?.remove(playground!!.id)
                }
            }
        }

        downvote.setOnClickListener {
            val from = FloatArray(3)
            val to = FloatArray(3)

            Color.colorToHSV(Color.parseColor("#FF707070"), from)
            Color.colorToHSV(Color.parseColor("#FFf44150"), to)

            val playgroundIsUpVoted = upVotedPlaygrounds?.contains(playground?.id) ?: false
            val playgroundIsDownVoted = downVotedPlaygrounds?.contains(playground?.id) ?: false
            if (!playgroundIsDownVoted && !playgroundIsUpVoted) {

                if (playgroundIsUpVoted) {
                    animateImageButton(to, from, upvote)
                    upvote.scaleType = ImageView.ScaleType.CENTER
                    if (playground != null) {
                        upVotedPlaygrounds?.add(playground!!.id)
                    }
                }

                animateImageButton(from, to, it as ImageButton)
                it.scaleType = ImageView.ScaleType.CENTER_CROP
                if (playground != null) {
                    downVotedPlaygrounds?.add(playground!!.id)
                }
            } else {

                animateImageButton(to, from, it as ImageButton)
                it.scaleType = ImageView.ScaleType.CENTER
                if (playground != null) {
                    downVotedPlaygrounds?.remove(playground!!.id)
                }
            }
        }

        playground_remarks.setOnClickListener {
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

    private fun animateImageButton(from: FloatArray, to: FloatArray, imageButton: ImageButton) {

        val anim = ValueAnimator.ofFloat(0.0f, 1.0f)
        anim.duration = 1000

        val hsv = FloatArray(3)

        anim.addUpdateListener {
            hsv[0] = from[0] + (to[0] - from[0]) * it.animatedFraction
            hsv[1] = from[1] + (to[1] - from[1]) * it.animatedFraction
            hsv[2] = from[2] + (to[2] - from[2]) * it.animatedFraction

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

    // Hold a reference to the current animator,
    // so that it can be canceled mid-way.
    private var mCurrentAnimator: Animator? = null

    // The system "short" animation time duration, in milliseconds. This
    // duration is ideal for subtle animations or animations that occur
    // very frequently.
    private var mShortAnimationDuration: Int = 0

    private fun zoomImageFromThumb(thumbView: View, imageResId: String) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        mCurrentAnimator?.cancel()



        Glide.with(this)
            .load(imageResId)
            .into(expanded_image)

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        val startBoundsInt = Rect()
        val finalBoundsInt = Rect()
        val globalOffset = Point()

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBoundsInt)

        container.getGlobalVisibleRect(finalBoundsInt, globalOffset)
        startBoundsInt.offset(-globalOffset.x, -globalOffset.y)
        finalBoundsInt.offset(-globalOffset.x, -globalOffset.y)

        val startBounds = RectF(startBoundsInt)
        val finalBounds = RectF(finalBoundsInt)

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        val startScale: Float
        if ((finalBounds.width() / finalBounds.height() > startBounds.width() / startBounds.height())) {
            // Extend start bounds horizontally
            startScale = startBounds.height() / finalBounds.height()
            val startWidth: Float = startScale * finalBounds.width()
            val deltaWidth: Float = (startWidth - startBounds.width()) / 2
            startBounds.left -= deltaWidth.toInt()
            startBounds.right += deltaWidth.toInt()
        } else {
            // Extend start bounds vertically
            startScale = startBounds.width() / finalBounds.width()
            val startHeight: Float = startScale * finalBounds.height()
            val deltaHeight: Float = (startHeight - startBounds.height()) / 2f
            startBounds.top -= deltaHeight.toInt()
            startBounds.bottom += deltaHeight.toInt()
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.alpha = 0f
        expanded_image.visibility = View.VISIBLE
        list_item_container.alpha = 0.1f

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expanded_image.pivotX = 0f
        expanded_image.pivotY = 0f

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        mCurrentAnimator = AnimatorSet().apply {
            play(
                ObjectAnimator.ofFloat(
                    expanded_image,
                    View.X,
                    startBounds.left,
                    finalBounds.left
                )
            ).apply {
                with(ObjectAnimator.ofFloat(expanded_image, View.Y, startBounds.top, finalBounds.top))
                with(ObjectAnimator.ofFloat(expanded_image, View.SCALE_X, startScale, 1f))
                with(ObjectAnimator.ofFloat(expanded_image, View.SCALE_Y, startScale, 1f))
            }
            duration = mShortAnimationDuration.toLong()
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation: Animator) {
                    mCurrentAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    mCurrentAnimator = null
                }
            })
            start()
        }

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        expanded_image.setOnClickListener {
            mCurrentAnimator?.cancel()

            // Animate the four positioning/sizing properties in parallel,
            // back to their original values.
            mCurrentAnimator = AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(expanded_image, View.X, startBounds.left)).apply {
                    with(ObjectAnimator.ofFloat(expanded_image, View.Y, startBounds.top))
                    with(ObjectAnimator.ofFloat(expanded_image, View.SCALE_X, startScale))
                    with(ObjectAnimator.ofFloat(expanded_image, View.SCALE_Y, startScale))
                }
                duration = mShortAnimationDuration.toLong()
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {
                        thumbView.alpha = 1f
                        expanded_image.visibility = View.GONE
                        list_item_container.alpha = 1f
                        mCurrentAnimator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        thumbView.alpha = 1f
                        expanded_image.visibility = View.GONE
                        list_item_container.alpha = 1f
                        mCurrentAnimator = null
                    }
                })
                start()
            }
        }
    }
}
