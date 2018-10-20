package de.borken.playgrounds.borkenplaygrounds

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.gson.JsonObject
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import de.borken.playgrounds.borkenplaygrounds.models.tryParsePlaygrounds
import kotlinx.android.synthetic.main.activity_playground.*


class PlaygroundActivity : AppCompatActivity(), PlaygroundElementListDialogFragment.Listener, PlaygroundListDialogFragment.Listener {

    override fun onPlaygroundClicked(position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPlaygroundElementClicked(position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var map: MapboxMap? = null
    private val CODE_AUTOCOMPLETE = 1
    private lateinit var autocompleteLocation: List<CarmenFeature>

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

        Mapbox.getInstance(this, getString(R.string.access_token))
        mapView.onCreate(savedInstanceState)
        mapView.setStyleUrl("mapbox://styles/tbuning/cjn4rntv80h002rnwdnsnfp39")
        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap

            mapboxMap.setOnMarkerClickListener {

                PlaygroundListDialogFragment.newInstance(30).show(supportFragmentManager, "dialog")

                true
            }

            val localizationPlugin = LocalizationPlugin(mapView!!, mapboxMap)

            try {
                localizationPlugin.matchMapLanguageWithDeviceDefault()
            } catch (exception: RuntimeException) {

            }
        }

        initSearchFab()

        initFilterFab()

        initLocations()
    }

    private fun initFilterFab() {

        filterButton.setOnClickListener {

            PlaygroundElementListDialogFragment.newInstance(30).show(supportFragmentManager, "dialog")
        }
    }

    private fun initLocations() {

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = settings
        db.collection("playgrounds")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    this.autocompleteLocation = tryParsePlaygrounds(task.result!!).map {
                        CarmenFeature.builder().text(it.name)
                            .placeName(it.name)
                            .geometry(it.location)
                            .id(it.name)
                            .properties(JsonObject())
                            .build()
                    }
                }
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == CODE_AUTOCOMPLETE) {

            // Retrieve selected location's CarmenFeature
            val selectedCarmenFeature = PlaceAutocomplete.getPlace(data)

            // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above
            val featureCollection = FeatureCollection.fromFeatures(
                arrayOf<Feature>(Feature.fromJson(selectedCarmenFeature.toJson()))
            )

            map?.getSourceAs<GeoJsonSource>("playground-borken")?.setGeoJson(featureCollection)

            // Move map camera to the selected location
            val newCameraPosition = CameraPosition.Builder()
                .target(
                    LatLng(
                        (selectedCarmenFeature.geometry() as Point).latitude(),
                        (selectedCarmenFeature.geometry() as Point).longitude()
                    )
                )
                .zoom(14.0)
                .build()
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition), 4000)
        }
    }


    public override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }
}
