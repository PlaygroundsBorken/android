package de.borken.playgrounds.borkenplaygrounds.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import com.bumptech.glide.Glide
import de.borken.playgrounds.borkenplaygrounds.fetchAvatarSettings
import de.borken.playgrounds.borkenplaygrounds.models.AvatarSetting
import de.borken.playgrounds.borkenplaygrounds.models.AvatarSettings
import de.borken.playgrounds.borkenplaygrounds.models.User
import de.borken.playgrounds.borkenplaygrounds.playgroundApp
import kotlinx.android.synthetic.main.sample_avatar_view.*




class AvatarViewDialog : DialogFragment() {

    private var avatarURL: String = getDefaultAvatarURL()

    private var activeUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(de.borken.playgrounds.borkenplaygrounds.R.layout.sample_avatar_view, container, false)
    }

    private var avatarSettings: AvatarSettings? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activeUser = context?.applicationContext?.playgroundApp?.activeUser

        avatarSettings = context?.applicationContext?.fetchAvatarSettings
        if (activeUser?.avatarURL !== null) {
            avatarURL = activeUser?.avatarURL!!
        }

        avatarSettings?.setFromAvatarUrl(avatarURL)
        avatarSettings?.avatarSetting?.forEach {

            createSpinner(it, avatarSettingsWrapper)
        }

        setAvatar()
        generateAvatar.setOnClickListener {

            super.dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {

        saveAvatar()

        val activity = activity
        if (activity is DialogInterface.OnDismissListener) {
            (activity as DialogInterface.OnDismissListener).onDismiss(dialog)
        }
        super.onDismiss(dialog)
    }

    private fun createSpinner(avatarSetting: AvatarSetting, wrapper: LinearLayout) {

        val spinnerArray = ArrayList<String>()

        var selectedIndex = 0
        avatarSetting.options.forEachIndexed { index, bodyPart ->
            spinnerArray.add(bodyPart.value)

            if (bodyPart.selected) {
                selectedIndex = index
            }
        }

        if (context !== null) {
            val spinner = Spinner(context)
            val spinnerArrayAdapter =
                ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, spinnerArray)
            spinner.adapter = spinnerArrayAdapter

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                    val selectedValue = avatarSetting.options[id.toInt()].value

                    if (avatarSettings !== null) {
                        avatarSettings!!.selectAvatarSetting(avatarSetting.body_part, selectedValue)
                        createAvatarUrl(avatarSettings!!)
                    }
                }
            }
            spinner.setSelection(selectedIndex)
            spinner.prompt = avatarSetting.body_part
            wrapper.addView(spinner)
        }
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
}