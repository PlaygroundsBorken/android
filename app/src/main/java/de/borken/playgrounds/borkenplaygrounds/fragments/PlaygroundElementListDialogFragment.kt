package de.borken.playgrounds.borkenplaygrounds.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.borken.playgrounds.borkenplaygrounds.databinding.FragmentPlaygroundelementListDialogBinding
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import de.borken.playgrounds.borkenplaygrounds.views.PlaygroundElementListView
import java.io.Serializable

/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    PlaygroundElementListDialogFragment.newInstance().show(supportFragmentManager, "dialog")
 * </pre>
 *
 * You activity (or fragment) needs to implement [PlaygroundElementListDialogFragment.Listener].
 */
class PlaygroundElementListDialogFragment : BottomSheetDialogFragment(), PlaygroundElementListView.Listener {

    private var selectedElements: MutableSet<PlaygroundElement> = mutableSetOf()
    private var mListener: Listener? = null
    private var _binding: FragmentPlaygroundelementListDialogBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaygroundelementListDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val playgroundList = arguments?.getSerializable("playground_element") as? List<*>

        selectedElements.addAll(playgroundList?.filterIsInstance<PlaygroundElement>()?.map {
            it
        }.orEmpty())
        
        if (selectedElements.isNullOrEmpty())
            binding.list.setup(null, this)
        else
            binding.list.setup(selectedElements.toList(), this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parent = parentFragment
        mListener = if (parent != null) {
            parent as Listener
        } else {
            context as Listener
        }
    }

    override fun onDetach() {
        mListener?.onPlaygroundElementsSelected(selectedElements.toList())
        mListener = null
        super.onDetach()
    }

    override fun playgroundElementSelected(element: PlaygroundElement) {

        selectedElements.add(element)
    }

    override fun playgroundElementDeselected(element: PlaygroundElement) {

        selectedElements.remove(element)
    }

    companion object {

        fun newInstance(selectedElements: List<PlaygroundElement>): PlaygroundElementListDialogFragment =
            PlaygroundElementListDialogFragment().apply {
                arguments = Bundle().apply {
                    //val string = getString(R.string.playground_element)
                    putSerializable("playground_element", selectedElements as Serializable)
                }
            }
    }

    interface Listener {
        fun onPlaygroundElementsSelected(elements: List<PlaygroundElement>)
    }
}
