package de.borken.playgrounds.borkenplaygrounds

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.PersistableBundle
import android.preference.PreferenceManager
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.JsonObject
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin
import com.mapbox.mapboxsdk.plugins.localization.MapLocale
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.tapadoo.alerter.Alerter
import de.borken.playgrounds.borkenplaygrounds.fragments.AvatarViewDialog
import de.borken.playgrounds.borkenplaygrounds.fragments.PlaygroundListDialogFragment
import de.borken.playgrounds.borkenplaygrounds.models.Playground
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import kotlinx.android.synthetic.main.activity_playground.*

open class BaseMapboxActivity : AppCompatActivity(), LocationListener {

    private val BOUND_CORNER_NW = LatLng(52.0, 7.0)
    private val BOUND_CORNER_SE = LatLng(51.6, 6.6)
    private val RESTRICTED_BOUNDS_AREA = LatLngBounds.Builder()
        .include(BOUND_CORNER_NW)
        .include(BOUND_CORNER_SE)
        .build()

    private var meMarker: Marker? = null
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

    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: Int = 199

    private var locationUpdatesAreAllowed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    makeUseOfNewLocation(location)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->

                    if (location !== null) {

                        lastKnownLocation = location
                        makeUseOfNewLocation(location)
                    }
                }
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle(R.string.title_location_permission)
                    .setMessage(R.string.text_location_permission)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        ActivityCompat.requestPermissions(
                            this@BaseMapboxActivity,
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ),
                            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                        )
                    }
                    .create()
                    .show()


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }
        }

        var accessToken = this.applicationContext.fetchMapboxAccessToken
        if (accessToken.isEmpty()) {
            accessToken = getString(R.string.access_token)
        }

        var mapboxUrl = this.applicationContext.fetchMapboxUrl
        if (mapboxUrl.isEmpty()) {

            mapboxUrl = getString(R.string.MAPBOX_STYLE)
        }
        Mapbox.getInstance(this, accessToken)
        mapView.onCreate(savedInstanceState)
        mapView.setStyleUrl(mapboxUrl)
        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap

            if (loadedPlaygrounds.isNotEmpty())
                addMarkersToMap(mapboxMap, loadedPlaygrounds)

            mapboxMap.setOnMarkerClickListener {

                val playground = this.markerToPlayground[it.id]

                when {
                    playground !== null -> {
                        PlaygroundListDialogFragment.newInstance(playground).show(supportFragmentManager, "dialog")
                        true
                    }
                    meMarker?.id == it.id -> {
                        loadedPlaygrounds.filter { playground1 ->
                            val playgroundLocation = Location("point B")
                            playgroundLocation.latitude = playground1.location.latitude()
                            playgroundLocation.longitude = playground1.location.longitude()

                            val playgroundLocationA = Location("point A")
                            playgroundLocationA.latitude = meMarker?.position?.latitude ?: 0.0
                            playgroundLocationA.longitude = meMarker?.position?.longitude ?: 0.0
                            playgroundLocationA.distanceTo(playgroundLocation) < 250
                        }.first {
                            PlaygroundListDialogFragment.newInstance(it).show(supportFragmentManager, "dialog")
                            true
                        }
                        true
                    }
                    else -> false
                }
            }
            mapboxMap.setLatLngBoundsForCameraTarget(RESTRICTED_BOUNDS_AREA)

            val localizationPlugin = LocalizationPlugin(mapView!!, mapboxMap)
            localizationPlugin.setMapLanguage(MapLocale(MapLocale.GERMAN))

            try {
                localizationPlugin.matchMapLanguageWithDeviceDefault()
            } catch (exception: RuntimeException) {

            }

            if (lastKnownLocation != null) {
                makeUseOfNewLocation(lastKnownLocation!!)
            }
        }

        initLocations()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        //requestLocation()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val disabledGPS = sharedPreferences.getBoolean(getString(R.string.disabled_gps), false)

        if (!disabledGPS) {
            val builder = LocationSettingsRequest.Builder()
            val locationRequest = createLocationRequest()
            if (locationRequest !== null) {
                builder.addLocationRequest(locationRequest)
                val client: SettingsClient = LocationServices.getSettingsClient(this)
                val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

                task.addOnSuccessListener {
                    locationUpdatesAreAllowed = true
                }

                task.addOnFailureListener { exception ->
                    if (exception is ResolvableApiException) {
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        stopLocationUpdates()
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            exception.startResolutionForResult(
                                this@BaseMapboxActivity,
                                MY_PERMISSIONS_REQUEST_LOCATION
                            )
                        } catch (sendEx: IntentSender.SendIntentException) {
                            // Ignore the error.
                        }
                    }
                }
            }
        }
    }

    private var lastKnownLocation: Location? = null

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

            if (this.markerToPlayground.values.contains(it)) {
                this.markerToPlayground.remove(addMarker.id)
            }
            this.markerToPlayground[addMarker.id] = it
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

                    if (task.result !== null) {
                        this.autocompleteLocation = Playground.tryParsePlaygrounds(task.result!!)?.map {

                            loadedPlaygrounds.add(it)

                            CarmenFeature.builder().text(it.name)
                                .placeName(it.name)
                                .geometry(it.location)
                                .id(it.name)
                                .properties(JsonObject())
                                .build()
                        }.orEmpty()
                    }

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
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (resultCode == Activity.RESULT_OK) {

                // permission was granted, yay! Do the
                // location-related task you need to do.
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) {

                    startLocationUpdates()
                }

            } else {
                stopLocationUpdates()
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
                with(sharedPreferences.edit()) {
                    putBoolean(getString(R.string.disabled_gps), true)
                    apply()
                }
            }
        }
    }

    private val MY_PERMISSIONS_REQUEST_LOCATION: Int = 99

    private var icon: Icon? = null

    private fun makeUseOfNewLocation(location: Location) {

        val activeUser = this.applicationContext.playgroundApp.activeUser
        var avatarURL = AvatarViewDialog.getDefaultAvatarURL()
        if (activeUser?.avatarURL !== null) {
            avatarURL = activeUser.avatarURL
        }

        loadedPlaygrounds.filter { playground ->
            val playgroundLocation = Location("point B")
            playgroundLocation.latitude = playground.location.latitude()
            playgroundLocation.longitude = playground.location.longitude()

            val alreadyVisited = activeUser?.mVisitedPlaygrounds?.contains(playground.id) ?: false

            location.distanceTo(playgroundLocation) < 250 && !alreadyVisited
        }.forEach {
            activeUser?.mVisitedPlaygrounds?.add(it.id)
            activeUser?.update()
            showNotificationAlert(
                activeUser?.mVisitedPlaygrounds?.count() ?: 0,
                this,
                supportFragmentManager
            )
        }

        if (icon == null) {
            Glide.with(this /* context */)
                .asBitmap()
                .load(avatarURL)
                .into<SimpleTarget<Bitmap>>(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                        icon = IconFactory.getInstance(this@BaseMapboxActivity).fromBitmap(resource)

                        moveMeMarker(location)
                    }
                })
        } else {

            moveMeMarker(location)
        }
    }

    private fun moveMeMarker(location: Location) {
        if (meMarker !== null)
            map?.removeMarker(meMarker!!)

        meMarker = map?.addMarker(
            MarkerOptions().position(
                LatLng(location.latitude, location.longitude)
            ).icon(icon)
        )

        val builder = LatLngBounds.Builder()
        this@BaseMapboxActivity.playgroundsOnMap.forEach {
            val latLng = LatLng(
                it.location.latitude(),
                it.location.longitude()
            )

            builder.include(latLng)
        }

        if (this@BaseMapboxActivity.playgroundsOnMap.size > 1) {
            builder.include(LatLng(location.latitude, location.longitude))
            map?.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150))
        } else {

            val newCameraPosition = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(14.0)
                .build()
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition), 4000)
        }
    }

    private fun showNotificationAlert(count: Int, activity: FragmentActivity, fragmentManager: FragmentManager?) {

        val notifications = this.applicationContext?.fetchPlaygroundNotifications

        try {
            val notification = notifications?.visitedPlaygroundsNotifications?.first { it.visitedPlaygrounds == count }

            if (notification !== null) {

                Alerter.create(activity)
                    .setTitle(notification.title)
                    .setText(notification.text)
                    .setDuration(10000)
                    .enableSwipeToDismiss()
                    .addButton(
                        "Avatar ändern",
                        R.style.AlertButton,
                        View.OnClickListener {
                            AvatarViewDialog.newInstance().show(supportFragmentManager, "dialog")
                        }
                    )
                    .show()
            }
        } catch (exception: NoSuchElementException) {

        }
    }

    private lateinit var locationCallback: LocationCallback

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun createLocationRequest(): LocationRequest? {
        return LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            smallestDisplacement = 50.0f
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
    }

    private fun startLocationUpdates() {

        if (!locationUpdatesAreAllowed) {

            return
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val disabledGPS = sharedPreferences.getBoolean(getString(R.string.disabled_gps), false)

        if (disabledGPS) {
            stopLocationUpdates()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                createLocationRequest(),
                locationCallback,
                null /* Looper */
            )
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle(R.string.title_location_permission)
                    .setMessage(R.string.text_location_permission)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        ActivityCompat.requestPermissions(
                            this@BaseMapboxActivity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                        )
                    }
                    .create()
                    .show()


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView?.onResume()

        startLocationUpdates()
    }

    public override fun onPause() {
        super.onPause()
        mapView?.onPause()
        stopLocationUpdates()
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

        locationManager.removeUpdates(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }


    override fun onLocationChanged(location: Location?) {

        if (location !== null) {
            makeUseOfNewLocation(location)
        }
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
    }

    override fun onProviderEnabled(p0: String?) {
    }

    override fun onProviderDisabled(p0: String?) {
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty()
                && grantResults[0] != PackageManager.PERMISSION_GRANTED
            ) {
                stopLocationUpdates()
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
                with(sharedPreferences.edit()) {
                    putBoolean(getString(R.string.disabled_gps), true)
                    apply()
                }
            }
        }
    }
}