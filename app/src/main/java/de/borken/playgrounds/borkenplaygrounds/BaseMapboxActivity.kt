package de.borken.playgrounds.borkenplaygrounds

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.JsonObject
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import de.borken.playgrounds.borkenplaygrounds.models.Playground
import de.borken.playgrounds.borkenplaygrounds.models.tryParsePlaygrounds
import kotlinx.android.synthetic.main.activity_playground.*

open class BaseMapboxActivity : AppCompatActivity() {

    private var map: MapboxMap? = null
    private val markerToPlayground = mutableMapOf<Long, Playground>()
    protected val CODE_AUTOCOMPLETE = 1
    protected lateinit var autocompleteLocation: List<CarmenFeature>

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        Mapbox.getInstance(this, getString(R.string.access_token))
        mapView.onCreate(savedInstanceState)
        mapView.setStyleUrl(getString(R.string.MAPBOX_STYLE))
        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap

            mapboxMap.setOnMarkerClickListener {

                val playground = this.markerToPlayground[it.id]

                if (playground !== null) {
                    PlaygroundListDialogFragment.newInstance(playground).show(supportFragmentManager, "dialog")
                    true
                } else
                    false
            }

            val localizationPlugin = LocalizationPlugin(mapView!!, mapboxMap)

            try {
                localizationPlugin.matchMapLanguageWithDeviceDefault()
            } catch (exception: RuntimeException) {

            }
        }

        initLocations()
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
                    this.autocompleteLocation = tryParsePlaygrounds(task.result!!)?.map {

                        val addedMarker = map?.addMarker(
                            MarkerOptions().position(
                                LatLng(
                                    it.location.latitude(),
                                    it.location.longitude()
                                )
                            ).title(it.name).snippet(it.description.orEmpty())
                        )

                        if (addedMarker !== null)
                            markerToPlayground[addedMarker.id] = it

                        CarmenFeature.builder().text(it.name)
                            .placeName(it.name)
                            .geometry(it.location)
                            .id(it.name)
                            .properties(JsonObject())
                            .build()
                    }.orEmpty()
                }
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