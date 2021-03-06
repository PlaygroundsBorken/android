package de.borken.playgrounds.borkenplaygrounds.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.glide.slider.library.SliderLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.tapadoo.alerter.Alerter
import de.borken.playgrounds.borkenplaygrounds.R
import de.borken.playgrounds.borkenplaygrounds.animation.ZoomAnimator
import de.borken.playgrounds.borkenplaygrounds.databinding.FragmentPlaygroundListDialogBinding
import de.borken.playgrounds.borkenplaygrounds.fetchPlaygroundNotifications
import de.borken.playgrounds.borkenplaygrounds.glide.PlaygroundSliderView
import de.borken.playgrounds.borkenplaygrounds.models.Playground
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import de.borken.playgrounds.borkenplaygrounds.models.Remark
import de.borken.playgrounds.borkenplaygrounds.models.User
import de.borken.playgrounds.borkenplaygrounds.playgroundApp


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

    private var _binding: FragmentPlaygroundListDialogBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun playgroundElementsLoaded(playgroundElements: List<PlaygroundElement>) {

        playgroundElements.forEach {
            binding.playgroundElements.playgroundElementAdded(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Retrieve and cache the system's default "short" animation time.
        _binding = FragmentPlaygroundListDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var playground: Playground? = null

    private var fusedLocationClient: FusedLocationProviderClient? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        this.playground = arguments?.getSerializable("PLAYGROUND_LIST") as? Playground

        binding.playgroundName.text = playground?.name

        setPlaygroundDescription()

        setPlaygroundRating()

        setPlaygroundImages(view)

        playground?.loadPlaygroundElements(this)

        val activeUser = context?.applicationContext?.playgroundApp?.activeUser

        setupUpVotingButton(activeUser)

        setupDownVotingButton(activeUser)

        binding.playgroundRemarks.setOnClickListener {
            remarksClickHandler(view)
        }

        setupGeoFencing(activeUser)
    }

    private fun setupDownVotingButton(activeUser: User?) {
        val downVotedPlaygrounds = activeUser?.mDownVotedPlaygrounds


        val isDownVoted = downVotedPlaygrounds?.contains(playground?.id)
        if (isDownVoted != null && isDownVoted) {

            binding.downvote.backgroundTintList = ColorStateList.valueOf(Color.RED)
        }

        binding.downvote.setOnClickListener {
            voteClickHandler(false)
        }
    }

    private fun setupUpVotingButton(activeUser: User?) {
        val upVotedPlaygrounds = activeUser?.mUpVotedPlaygrounds
        val isUpVoted = upVotedPlaygrounds?.contains(playground?.id)
        if (isUpVoted != null && isUpVoted) {

            binding.upvote.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
        }
        binding.upvote.setOnClickListener {
            voteClickHandler(true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupGeoFencing(activeUser: User?) {
        if (activity != null)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (fusedLocationClient != null) {

            fusedLocationClient!!.lastLocation
                .addOnSuccessListener { location: Location? ->

                    val playgroundLocation = Location("point B")
                    playgroundLocation.latitude = playground?.location?.latitude() ?: 0.0
                    playgroundLocation.longitude = playground?.location?.longitude() ?: 0.0

                    val distance = location?.distanceTo(playgroundLocation)

                    if (distance === null || distance > 250) {
                        return@addOnSuccessListener
                    }

                    binding.playgroundButtonContainer.visibility = VISIBLE

                    val playgroundAlreadyVisited = activeUser?.mVisitedPlaygrounds?.contains(playground?.id)
                    if (playgroundAlreadyVisited === null || playgroundAlreadyVisited || playground !== null) {
                        return@addOnSuccessListener
                    }

                    activeUser.mVisitedPlaygrounds.add(playground!!.id)

                    activeUser.update()
                    if (activity !== null) {

                        showNotificationAlert(
                            activeUser.mVisitedPlaygrounds.count(),
                            requireActivity(),
                            parentFragmentManager
                        )
                    }
                }
        }
    }

    private fun showNotificationAlert(count: Int, activity: FragmentActivity, fragmentManager: FragmentManager?) {

        val notifications = context?.applicationContext?.fetchPlaygroundNotifications

        try {
            val notification = notifications?.visitedPlaygroundsNotifications?.first { it.visitedPlaygrounds == count }

            if (notification !== null) {

                Alerter.create(activity)
                    .setTitle(notification.title)
                    .setText(notification.text)
                    .setDuration(10000)
                    .enableSwipeToDismiss()
                    .addButton(
                        "Avatar ändern",
                        R.style.AlertButton
                    ) {
                        if (fragmentManager !== null) {
                            AvatarViewDialog.newInstance().show(fragmentManager, "dialog")
                        }
                    }
                    .show()
            }
        } catch (exception: NoSuchElementException) {

        }
    }

    private fun setPlaygroundImages(view: View) {
        playground?.images.orEmpty().forEach { image ->

            val sliderView = PlaygroundSliderView(view.context)
            sliderView
                .image(image)
                .setProgressBarVisible(true)

            sliderView.setOnSliderClickListener {
                it as PlaygroundSliderView

                ZoomAnimator(this.requireContext()).zoomImageFromThumb(
                    binding.expandedImage,
                    it.imageView,
                    image,
                    requireActivity().findViewById(R.id.container),
                    binding.listItemContainer
                )
            }

            binding.playgroundImagesSlider.addSlider(sliderView)
        }

        binding.playgroundImagesSlider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom)
        binding.playgroundImagesSlider.setDuration(10000)
    }

    private fun setPlaygroundRating() {
        if (playground?.rating?.toFloat() !== null)
            binding.playgroundRating.rating = playground!!.rating!!.toFloat()
    }

    private fun setPlaygroundDescription() {
        val description = playground?.description.orEmpty().replace("<li>", "&#8226;&nbsp;").replace("</li>", "<br/>")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            binding.playgroundDescription.text = Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT)
        } else {
            binding.playgroundDescription.text = Html.fromHtml(description)
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
            binding.upvote.backgroundTintList = ColorStateList.valueOf(Color.GREEN)
            binding.downvote.backgroundTintList = ColorStateList.valueOf(Color.GRAY)

        } else {
            downVotedPlaygrounds?.add(playground!!.id)
            upVotedPlaygrounds?.remove(playground!!.id)
            binding.upvote.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            binding.downvote.backgroundTintList = ColorStateList.valueOf(Color.RED)
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
            resources.getDimensionPixelSize(R.dimen.dialog_margin)
        params.rightMargin =
            resources.getDimensionPixelSize(R.dimen.dialog_margin)
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
