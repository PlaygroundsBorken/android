package de.borken.playgrounds.borkenplaygrounds

import android.os.Bundle
import com.github.paolorotolo.appintro.AppIntro2


class IntroActivity: AppIntro2() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide Skip/Done button.
        showSkipButton(false)
        isProgressButtonEnabled = false

        // Turn vibration on and set intensity.
        // NOTE: you will probably need to ask VIBRATE permission in Manifest.
        setVibrate(true)
        setVibrateIntensity(30)
    }
}