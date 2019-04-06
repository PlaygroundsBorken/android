package de.borken.playgrounds.borkenplaygrounds.models

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

open class BitmapTarget: CustomTarget<Bitmap>() {

    override fun onLoadCleared(placeholder: Drawable?) {
    }

    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

    }

}