package de.borken.playgrounds.borkenplaygrounds.fragments

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.borken.playgrounds.borkenplaygrounds.R

class AvatarViewDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Retrieve and cache the system's default "short" animation time.

        return inflater.inflate(R.layout.sample_avatar_view, container, false)
    }

    companion object {

        fun newInstance(): AvatarViewDialog =

            AvatarViewDialog().apply {
                arguments = Bundle().apply {

                }
            }
    }
}