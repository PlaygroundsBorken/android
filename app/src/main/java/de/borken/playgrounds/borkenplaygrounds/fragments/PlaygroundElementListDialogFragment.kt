package de.borken.playgrounds.borkenplaygrounds.fragments

import android.content.Context
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import de.borken.playgrounds.borkenplaygrounds.R
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import de.borken.playgrounds.borkenplaygrounds.views.PlaygroundElementListView
import kotlinx.android.synthetic.main.fragment_playgroundelement_list_dialog.*
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_playgroundelement_list_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val playgroundList = arguments?.getSerializable("playground_element") as? List<*>

        selectedElements.addAll(playgroundList?.filter { it is PlaygroundElement }?.map {
            it as PlaygroundElement
        }.orEmpty())
        
        if (selectedElements.isNullOrEmpty())
            list.setup(null, this)
        else
            list.setup(selectedElements.toList(), this)
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
