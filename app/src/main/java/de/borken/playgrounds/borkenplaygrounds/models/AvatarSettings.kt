package de.borken.playgrounds.borkenplaygrounds.models

import com.google.gson.Gson

class AvatarSettings(val avatarSetting: List<AvatarSetting>) {

    fun selectAvatarSetting(body_part: String, option_value: String) {

        avatarSetting.filter { it.body_part == body_part }.forEach { setting ->

            setting.options.forEach {

                it.selected = it.value === option_value
            }
        }
    }

    fun setFromAvatarUrl(avatarURL: String) {

        avatarSetting.forEach {setting ->

            setting.options.forEach {

                it.selected = avatarURL.contains(setting.body_part + '=' + it.id)
            }
        }
    }

    companion object {

        fun fromJson(jsonObject: String): AvatarSettings {

            return Gson().fromJson(jsonObject, AvatarSettings::class.java)
        }
    }
}

class AvatarSetting(val body_part: String, val options: List<BodyPart>)

class BodyPart(val id: String, val value: String, var selected: Boolean = false)