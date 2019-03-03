package de.borken.playgrounds.borkenplaygrounds

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import com.bumptech.glide.Glide
import de.borken.playgrounds.borkenplaygrounds.fragments.AvatarViewDialog
import de.borken.playgrounds.borkenplaygrounds.models.AvatarSetting
import de.borken.playgrounds.borkenplaygrounds.models.AvatarSettings
import de.borken.playgrounds.borkenplaygrounds.models.User
import kotlinx.android.synthetic.main.sample_avatar_view.*

class AvatarActivity : AppCompatActivity() {

    private var avatarURL: String = AvatarViewDialog.getDefaultAvatarURL()
    private var activeUser: User? = null
    private var avatarSettings: AvatarSettings? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_avatar_view)

        activeUser = this.applicationContext?.playgroundApp?.activeUser

        avatarSettings = this.applicationContext?.fetchAvatarSettings
        if (activeUser?.avatarURL !== null) {
            avatarURL = activeUser?.avatarURL!!
        }

        avatarSettings?.setFromAvatarUrl(avatarURL)
        avatarSettings?.avatarSetting?.forEach {

            createSpinner(it, avatarSettingsWrapper)
        }

        setAvatar()
        generateAvatar.setOnClickListener {

            saveAvatar()
            finish()
        }
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

        val spinner = Spinner(this)
        val spinnerArrayAdapter =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, spinnerArray)
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
