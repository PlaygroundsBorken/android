package de.borken.playgrounds.borkenplaygrounds

import android.graphics.Color
import android.os.Bundle
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import kotlinx.android.synthetic.main.activity_playground.*


class PlaygroundActivity : BaseMapboxActivity(), PlaygroundElementListDialogFragment.Listener {

    override fun onPlaygroundElementClicked(position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)


        val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        // [START enable_dev_mode]
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setDeveloperModeEnabled(BuildConfig.DEBUG)
            .build()
        mFirebaseRemoteConfig.setConfigSettings(configSettings)
        // [END enable_dev_mode]

        initSearchFab()

        initFilterFab()
    }

    private fun initFilterFab() {

        filterButton.setOnClickListener {

            PlaygroundElementListDialogFragment.newInstance().show(supportFragmentManager, "dialog")
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
