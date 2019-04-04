package de.borken.playgrounds.borkenplaygrounds.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import de.borken.playgrounds.borkenplaygrounds.R
import kotlinx.android.synthetic.main.fragment_glide_image.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "slideUrl"

/**
 * A simple [Fragment] subclass.
 * Use the [GlideImageFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class GlideImageFragment : Fragment() {
    private var slideUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            slideUrl = it.getString(ARG_PARAM1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_glide_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (slideUrl !== null && activity !== null) {
            Glide.with(activity!! /* context */)
                .load(slideUrl)
                .into(slideImage)
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param slideUrl Parameter 1.
         * @return A new instance of fragment GlideImageFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(slideUrl: String) =
            GlideImageFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, slideUrl)
                }
            }
    }
}
