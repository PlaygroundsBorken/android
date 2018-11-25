package de.borken.playgrounds.borkenplaygrounds

import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import de.borken.playgrounds.borkenplaygrounds.fragments.PlaygroundElementListDialogFragment
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import kotlinx.android.synthetic.main.activity_playground.*


class PlaygroundActivity : BaseMapboxActivity(), PlaygroundElementListDialogFragment.Listener {

    private var selectedElements: List<PlaygroundElement>? = null

    override fun onPlaygroundElementsSelected(elements: List<PlaygroundElement>) {

        selectedElements = elements
        filterMarkers(elements)
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        onCreate(savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        initSearchFab()

        initFilterFab()
    }

    private fun initFilterFab() {

        filterButton.setOnClickListener {

            PlaygroundElementListDialogFragment.newInstance(selectedElements.orEmpty()).show(supportFragmentManager, "dialog")
        }
    }


    private fun initSearchFab() {

        searchButton?.setOnClickListener {

            val placeOptions = PlaceOptions.builder()
                .limit(10)
                .language("DE")
                .country("DE")
                .backgroundColor(Color.parseColor("#AAFFFFFF"))

            autocompleteLocation.forEach { feature ->
                placeOptions.addInjectedFeature(feature)
            }

            val options = placeOptions.build(PlaceOptions.MODE_CARDS)

            val intent = PlaceAutocomplete.IntentBuilder()
                .accessToken(getString(R.string.access_token))
                .placeOptions(options)
                .build(this)
            startActivityForResult(intent, CODE_AUTOCOMPLETE)
        }
    }


}
