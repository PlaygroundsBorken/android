package de.borken.playgrounds.borkenplaygrounds.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
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
import android.widget.FrameLayout
import com.glide.slider.library.SliderLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import de.borken.playgrounds.borkenplaygrounds.R
import de.borken.playgrounds.borkenplaygrounds.animation.ZoomAnimator
import de.borken.playgrounds.borkenplaygrounds.glide.PlaygroundSliderView
import de.borken.playgrounds.borkenplaygrounds.models.*
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
            if (playground_elements != null)
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        this.playground = arguments?.getSerializable("PLAYGROUND_LIST") as? Playground

        playground_name.text = playground?.name

        setPlaygroundDescription()

        setPlaygroundRating()

        setPlaygroundImages(view)

        playground?.loadPlaygroundElements(this)

        val activeUser = context?.applicationContext?.playgroundApp?.activeUser

        setupUpVotingButton(activeUser)

        setupDownVotingButton(activeUser)

        playground_remarks.setOnClickListener {
            remarksClickHandler(view)
        }

        //val mBehavior = BottomSheetBehavior.from(list)
        setupGeoFencing(activeUser)
    }

    private fun setupDownVotingButton(activeUser: User?) {
        val downVotedPlaygrounds = activeUser?.mDownVotedPlaygrounds


        val isDownVoted = downVotedPlaygrounds?.contains(playground?.id)
        if (isDownVoted != null && isDownVoted) {

            downvote.backgroundTintList = ColorStateList.valueOf(Color.RED)
        }

        downvote.setOnClickListener {
            voteClickHandler(false)
        }
    }

    private fun setupUpVotingButton(activeUser: User?) {
        val upVotedPlaygrounds = activeUser?.mUpVotedPlaygrounds
        val isUpVoted = upVotedPlaygrounds?.contains(playground?.id)
        if (isUpVoted != null && isUpVoted) {

            upvote.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
        }
        upvote.setOnClickListener {
            voteClickHandler(true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupGeoFencing(activeUser: User?) {
        if (activity != null)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)

        if (fusedLocationClient != null) {

            fusedLocationClient!!.lastLocation
                .addOnSuccessListener { location: Location? ->

                    val playgroundLocation = Location("point B")
                    playgroundLocation.latitude = playground?.location?.latitude() ?: 0.0
                    playgroundLocation.longitude = playground?.location?.longitude() ?: 0.0

                    val distance = location?.distanceTo(playgroundLocation)

                    if (distance === null || distance < 100) {
                        return@addOnSuccessListener
                    }

                    playground_button_container.visibility = VISIBLE

                    val playgroundAlreadyVisited = activeUser?.mVisitedPlaygrounds?.contains(playground?.id)
                    if (playgroundAlreadyVisited === null || playgroundAlreadyVisited || playground !== null) {
                        return@addOnSuccessListener
                    }

                    activeUser.mVisitedPlaygrounds.add(playground!!.id)

                    activeUser.update()
                    if (activity !== null) {
                        VisitedPlaygroundsNotifications().showNotification(
                            activeUser.mVisitedPlaygrounds.count(),
                            activity!!
                        )
                    }
                }
        }
    }

    private fun setPlaygroundImages(view: View) {
        playground?.images.orEmpty().forEach { image ->

            val sliderView = PlaygroundSliderView(view.context)
            sliderView
                .image(image)
                .setBackgroundColor(Color.WHITE)
                .setProgressBarVisible(true)

            sliderView.setOnSliderClickListener {
                it as PlaygroundSliderView

                ZoomAnimator(this.context!!).zoomImageFromThumb(
                    expanded_image,
                    it.imageView,
                    image,
                    activity!!.findViewById(R.id.container),
                    list_item_container
                )
            }

            playground_images_slider.addSlider(sliderView)
        }

        playground_images_slider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom)
        playground_images_slider.setDuration(10000)
    }

    private fun setPlaygroundRating() {
        if (playground?.rating?.toFloat() !== null)
            playground_rating.rating = playground!!.rating!!.toFloat()
    }

    private fun setPlaygroundDescription() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            playground_description.text =
                Html.fromHtml(playground?.description.orEmpty(), Html.FROM_HTML_MODE_COMPACT)
        } else {
            playground_description.text = Html.fromHtml(playground?.description.orEmpty())
        }
    }

    private fun voteClickHandler(
        isUpVote: Boolean
    ) {
        if (playground == null) {
            return
        }
        val activeUser = context?.applicationContext?.playgroundApp?.activeUser
        val upVotedPlaygrounds = activeUser?.mUpVotedPlaygrounds
        val downVotedPlaygrounds = activeUser?.mDownVotedPlaygrounds
        if (isUpVote) {

            upVotedPlaygrounds?.add(playground!!.id)
            downVotedPlaygrounds?.remove(playground!!.id)
            upvote.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
            downvote.backgroundTintList = ColorStateList.valueOf(Color.GRAY)

        } else {
            downVotedPlaygrounds?.add(playground!!.id)
            upVotedPlaygrounds?.remove(playground!!.id)
            upvote.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            downvote.backgroundTintList = ColorStateList.valueOf(Color.RED)
        }
        activeUser?.update()
    }

    private fun remarksClickHandler(view: View) {
        val builder = AlertDialog.Builder(view.context)
        builder.setTitle("Kommentieren")

        val input = EditText(view.context)
        val container = FrameLayout(view.context)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin =
            resources.getDimensionPixelSize(de.borken.playgrounds.borkenplaygrounds.R.dimen.dialog_margin)
        params.rightMargin =
            resources.getDimensionPixelSize(de.borken.playgrounds.borkenplaygrounds.R.dimen.dialog_margin)
        input.layoutParams = params
        input.inputType = InputType.TYPE_CLASS_TEXT
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton(
            "OK"
        ) { _, _ ->
            saveCustomerRemark(input.text.toString(), playground)

        }
        builder.setNegativeButton(
            "Abbrechen"
        ) { dialog, _ -> dialog.cancel() }

        builder.show()
    }


    private fun saveCustomerRemark(toString: String, playground: Playground?) {
        val activeUser = context?.applicationContext?.playgroundApp?.activeUser

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = settings
        val newRemark = Remark.newRemark(
            activeUser?.documentId,
            playground?.id,
            toString
        )
        if (newRemark != null) {
            db.collection("userRemarks").add(newRemark)
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
