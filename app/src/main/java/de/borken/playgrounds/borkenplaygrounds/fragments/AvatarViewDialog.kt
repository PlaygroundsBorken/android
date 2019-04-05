package de.borken.playgrounds.borkenplaygrounds.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import de.borken.playgrounds.borkenplaygrounds.fetchAvatarSettings
import de.borken.playgrounds.borkenplaygrounds.models.AvatarSetting
import de.borken.playgrounds.borkenplaygrounds.models.AvatarSettings
import de.borken.playgrounds.borkenplaygrounds.models.User
import de.borken.playgrounds.borkenplaygrounds.playgroundApp
import kotlinx.android.synthetic.main.sample_avatar_view.*

class AvatarViewDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(de.borken.playgrounds.borkenplaygrounds.R.layout.sample_avatar_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activeUser = activity?.applicationContext?.playgroundApp?.activeUser

        avatarSettings = activity?.applicationContext?.fetchAvatarSettings
        if (activeUser?.avatarURL !== null) {
            avatarURL = activeUser?.avatarURL!!
        }

        var visitedPlaygrounds = activeUser?.mVisitedPlaygrounds?.size

        if (visitedPlaygrounds == null) {

            visitedPlaygrounds = 0
        }
        val amount = steps[visitedPlaygrounds]
        avatarSettings?.setFromAvatarUrl(avatarURL)
        avatarSettings?.avatarSetting?.forEachIndexed { index, avatarSetting ->


            if (visitedPlaygrounds > 0) {

                if (amount != null) {
                    if (amount >= index) {
                        createSpinner(avatarSetting, avatarSettingsWrapper)
                    }
                } else {
                    createSpinner(avatarSetting, avatarSettingsWrapper)
                }
            }
        }

        setAvatar()
        generateAvatar.setOnClickListener {

            saveAvatar()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val displayMetrics = context?.resources?.displayMetrics

        if (displayMetrics !== null) {
            val dpHeight = displayMetrics.heightPixels * 0.9
            val dpWidth = displayMetrics.widthPixels * 0.9
            dialog?.window?.setLayout(dpWidth.toInt(), dpHeight.toInt())
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        saveAvatar()

        val activity = activity
        if (activity is DialogInterface.OnDismissListener) {
            (activity as DialogInterface.OnDismissListener).onDismiss(dialog)
        }
        super.onDismiss(dialog)
    }

    companion object {

        fun newInstance(): AvatarViewDialog =

            AvatarViewDialog().apply {
                arguments = Bundle().apply {

                }
            }

        fun getDefaultAvatarURL(): String {
            var avatarURL = "https://avataaars.io/png/260?"
            avatarURL += "&topType=NoHair"
            avatarURL += "&accessoriesType=Blank"
            avatarURL += "&clotheType=BlazerShirt"
            avatarURL += "&eyeType=Default"
            avatarURL += "&avatarStyle=Circle"

            return avatarURL
        }
    }
    private var avatarURL: String = AvatarViewDialog.getDefaultAvatarURL()
    private var activeUser: User? = null
    private var avatarSettings: AvatarSettings? = null

    private val steps = hashMapOf(1 to 1, 2 to 2, 3 to 3, 5 to 4, 8 to 5, 11 to 6, 15 to 7, 21 to 8, 25 to 9)

    private fun createSpinner(avatarSetting: AvatarSetting, wrapper: LinearLayout) {

        val spinnerArray = ArrayList<String>()

        var selectedIndex = 0
        avatarSetting.options.forEachIndexed { index, bodyPart ->
            spinnerArray.add(bodyPart.value)

            if (bodyPart.selected) {
                selectedIndex = index
            }
        }

        val spinner = Spinner(activity)

        val spinnerArrayAdapter =
            ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, spinnerArray)
        spinner.adapter = spinnerArrayAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                val index = id.toInt()
                if (avatarSetting.options.size >= index) {
                    val value = avatarSetting.options[index].value

                    if (value.isNotEmpty()) {

                        if (avatarSettings !== null) {
                            avatarSettings!!.selectAvatarSetting(avatarSetting.body_part, value)
                            createAvatarUrl(avatarSettings!!)
                        }
                    }
                }
            }
        }
        spinner.setSelection(selectedIndex)
        spinner.prompt = avatarSetting.body_part

        wrapper.addView(spinner)

    }

    private fun createAvatarUrl(avatarSettings: AvatarSettings) {

        avatarURL = "https://avataaars.io/png/260?"

        avatarSettings.avatarSetting.forEach { avatarSetting ->

            avatarURL += '&' + avatarSetting.body_part + '=' + avatarSetting.options.first { it.selected }.id
        }

        setAvatar()
    }

    private fun setAvatar() {
        Glide.with(this /* context */)
            .load(avatarURL)
            .into(avatarImageView)
    }

    private fun saveAvatar() {

        activeUser?.avatarURL = avatarURL
        activeUser?.update()
    }
}