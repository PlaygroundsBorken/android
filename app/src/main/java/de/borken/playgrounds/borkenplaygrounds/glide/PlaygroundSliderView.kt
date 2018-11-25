package de.borken.playgrounds.borkenplaygrounds.glide

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.glide.slider.library.R
import com.glide.slider.library.SliderTypes.BaseSliderView

class PlaygroundSliderView(context: Context) : BaseSliderView(context) {

    lateinit var imageView: AppCompatImageView

    override fun getView(): View {
        val v = LayoutInflater.from(this.context).inflate(R.layout.render_type_default, null as ViewGroup?)
        val target = v.findViewById<View>(R.id.glide_slider_image) as AppCompatImageView
        this.imageView = target
        this.bindEventAndShow(v, target)
        return v
    }
}