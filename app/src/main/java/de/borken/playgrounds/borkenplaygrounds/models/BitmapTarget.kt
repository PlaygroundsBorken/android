package de.borken.playgrounds.borkenplaygrounds.models

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

open class BitmapTarget: CustomTarget<Bitmap>() {

    override fun onLoadCleared(placeholder: Drawable?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}