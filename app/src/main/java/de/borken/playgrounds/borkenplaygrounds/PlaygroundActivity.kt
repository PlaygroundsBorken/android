package de.borken.playgrounds.borkenplaygrounds

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import de.borken.playgrounds.borkenplaygrounds.fragments.PlaygroundElementListDialogFragment
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement


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

        binding.moreButton.setOnClickListener {

            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initFilterFab() {

        binding.filterButton.setOnClickListener {

            PlaygroundElementListDialogFragment.newInstance(selectedElements.orEmpty()).show(supportFragmentManager, "dialog")
        }
    }

    private fun initSearchFab() {

        binding.searchButton.setOnClickListener {

            val placeOptions = PlaceOptions.builder()
                .limit(10)
                .language("de")
                .country("DE")
                .bbox(Point.fromLngLat(6.6, 51.6), Point.fromLngLat(7.0, 52.0))
                .backgroundColor(Color.parseColor("#AAFFFFFF"))

            if (autoCompleteIsInitialized()) {
                autocompleteLocation.forEach { feature ->
                    placeOptions.addInjectedFeature(feature)
                }
            }

            val options = placeOptions.build(PlaceOptions.MODE_CARDS)

            val accessToken= this.applicationContext.fetchMapboxAccessToken
            val intent = PlaceAutocomplete.IntentBuilder()
                .accessToken(accessToken)
                .placeOptions(options)
                .build(this)
            startActivityForResult(intent, codeAutocomplete)
        }
    }
}
