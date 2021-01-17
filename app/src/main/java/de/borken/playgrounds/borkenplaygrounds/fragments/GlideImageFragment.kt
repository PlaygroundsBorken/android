package de.borken.playgrounds.borkenplaygrounds.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import coil.load
import de.borken.playgrounds.borkenplaygrounds.databinding.FragmentGlideImageBinding

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
    private var _binding: FragmentGlideImageBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGlideImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (slideUrl !== null && activity !== null) {
            binding.slideImage.load(slideUrl)
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
