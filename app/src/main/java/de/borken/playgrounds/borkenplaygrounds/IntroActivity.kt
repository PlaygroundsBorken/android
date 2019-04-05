package de.borken.playgrounds.borkenplaygrounds

import android.os.Bundle
import com.github.paolorotolo.appintro.AppIntro2
import de.borken.playgrounds.borkenplaygrounds.fragments.GlideImageFragment


class IntroActivity: AppIntro2() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addSlide(GlideImageFragment.newInstance("https://res.cloudinary.com/tbuning/image/upload/c_thumb,w_1200,g_face/slides/Slider-01-Karte.webp"))
        addSlide(GlideImageFragment.newInstance("https://res.cloudinary.com/tbuning/image/upload/c_thumb,w_1200,g_face/slides/Slider-02-Suche.webp"))
        addSlide(GlideImageFragment.newInstance("https://res.cloudinary.com/tbuning/image/upload/c_thumb,w_1200,g_face/slides/Slider-03-Beschreibung.webp"))
        addSlide(GlideImageFragment.newInstance("https://res.cloudinary.com/tbuning/image/upload/c_thumb,w_1200,g_face/slides/Slider-04-Bewertung.webp"))

        // Hide Skip/Done button.
        showSkipButton(false)
        isProgressButtonEnabled = false
    }
}