package de.borken.playgrounds.borkenplaygrounds

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.JsonObject
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin
import com.mapbox.mapboxsdk.plugins.localization.MapLocale
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import de.borken.playgrounds.borkenplaygrounds.fragments.PlaygroundListDialogFragment
import de.borken.playgrounds.borkenplaygrounds.models.Playground
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import kotlinx.android.synthetic.main.activity_playground.*

open class BaseMapboxActivity : AppCompatActivity(), PermissionsListener {

    private var originLocation: Location? = null
    private var permissionsManager: PermissionsManager? = null
    private var map: MapboxMap? = null
    private val markerToPlayground: MutableMap<Long, Playground> = mutableMapOf()
    private val playgroundsOnMap = mutableSetOf<Playground>()
    private val loadedPlaygrounds = mutableListOf<Playground>()
    protected val CODE_AUTOCOMPLETE = 1
    protected lateinit var autocompleteLocation: List<CarmenFeature>

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        onCreate(savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)

        Mapbox.getInstance(this, getString(R.string.access_token))
        mapView.onCreate(savedInstanceState)
        mapView.setStyleUrl(getString(R.string.MAPBOX_STYLE))
        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap

            if (loadedPlaygrounds.isNotEmpty())
                addMarkersToMap(mapboxMap, loadedPlaygrounds)

            enableLocationComponent()
            mapboxMap.setOnMarkerClickListener {

                val playground = this.markerToPlayground[it.id]

                if (playground !== null) {
                    PlaygroundListDialogFragment.newInstance(playground).show(supportFragmentManager, "dialog")
                    true
                } else
                    false
            }

            val localizationPlugin = LocalizationPlugin(mapView!!, mapboxMap)
            localizationPlugin.setMapLanguage(MapLocale(MapLocale.GERMAN))

            try {
                localizationPlugin.matchMapLanguageWithDeviceDefault()
            } catch (exception: RuntimeException) {

            }
        }

        initLocations()
    }

    private fun addMarkersToMap(mapboxMap: MapboxMap, playgrounds: List<Playground>) {
        val bitmap = getBitmapFromVectorDrawable(this, R.mipmap.ic_launcher_round)
        val icon = IconFactory.getInstance(this).fromBitmap(bitmap)

        val builder = LatLngBounds.Builder()

        val filteredPlaygrounds = playgrounds.filter { it !in playgroundsOnMap }
        filteredPlaygrounds.forEach {

            val latLng = LatLng(
                it.location.latitude(),
                it.location.longitude()
            )

            val addMarker = mapboxMap.addMarker(
                MarkerOptions().position(
                    latLng
                ).title(it.name).snippet(it.description.orEmpty()).icon(icon)
            )

            if (!this.markerToPlayground.values.contains(it)) {
                this.markerToPlayground[addMarker.id] = it
            }
            this.playgroundsOnMap.add(it)
        }

        playgrounds.forEach {
            val latLng = LatLng(
                it.location.latitude(),
                it.location.longitude()
            )

            builder.include(latLng)
        }

        playgroundsOnMap.clear()
        playgroundsOnMap.addAll(playgrounds)

        if (playgrounds.size == 1) {
            val location = playgrounds.first().location
            mapboxMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude(), location.longitude())))
        } else if (playgrounds.size > 1) {
            mapboxMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150))
        }
    }

    protected fun filterMarkers(selectedPlaygroundElements: List<PlaygroundElement>) {

        val selectedPlaygroundElementIds = selectedPlaygroundElements.map { it.id }
        var activeMarkerToPlayground = markerToPlayground.mapNotNull { (id, playground) ->
            val containsPlaygroundElement = selectedPlaygroundElementIds.all { it in playground.mPlaygroundElements }

            if (containsPlaygroundElement) {
                Pair(id, playground)
            } else {
                null
            }
        }.toMap()

        if (selectedPlaygroundElements.isEmpty()) {
            activeMarkerToPlayground = markerToPlayground
        }

        map?.markers?.filter { !activeMarkerToPlayground.keys.contains(it.id) }?.forEach {

            map?.removeMarker(it)
        }

        if (map != null)
            addMarkersToMap(map!!, activeMarkerToPlayground.values.toList())
    }

    private fun initLocations() {

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = settings
        db.collection("playgrounds")
            .whereEqualTo("trash", false)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    this.autocompleteLocation = Playground.tryParsePlaygrounds(task.result!!)?.map {

                        loadedPlaygrounds.add(it)

                        CarmenFeature.builder().text(it.name)
                            .placeName(it.name)
                            .geometry(it.location)
                            .id(it.name)
                            .properties(JsonObject())
                            .build()
                    }.orEmpty()

                    if (map != null) {
                        addMarkersToMap(map!!, loadedPlaygrounds)
                    }
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


    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Activate the MapboxMap LocationComponent to show user location


            // Adding in LocationComponentOptions is also an optional parameter
            val options = LocationComponentOptions.builder(this)
                .elevation(5F)
                .accuracyAlpha(.6f)
                .accuracyColor(Color.RED)
                .foregroundDrawable(R.drawable.avataaars)
                .build()

            val locationComponent = map?.locationComponent


            locationComponent?.activateLocationComponent(this, options)
            locationComponent?.isLocationComponentEnabled = true
            // Set the component's camera mode
            locationComponent?.cameraMode = CameraMode.TRACKING
            originLocation = locationComponent?.lastKnownLocation

        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        permissionsManager?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {

        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent()
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