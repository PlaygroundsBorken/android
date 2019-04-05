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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.gson.JsonObject
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin
import com.mapbox.mapboxsdk.plugins.localization.MapLocale
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.tapadoo.alerter.Alerter
import de.borken.playgrounds.borkenplaygrounds.fragments.AvatarViewDialog
import de.borken.playgrounds.borkenplaygrounds.fragments.PlaygroundListDialogFragment
import de.borken.playgrounds.borkenplaygrounds.models.BitmapTarget
import de.borken.playgrounds.borkenplaygrounds.models.Playground
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import kotlinx.android.synthetic.main.activity_playground.*

open class BaseMapboxActivity : AppCompatActivity(), LocationListener {

    private val _meMarkerIcon = "memarker"
    private val _playgroundMarkerIcon = "playground"
    private val _boundCornerNE = LatLng(52.0, 7.0)
    private val _boundCornerSW = LatLng(51.6, 6.6)
    private val _restrictedBoundsArea = LatLngBounds.Builder()
        .include(_boundCornerNE)
        .include(_boundCornerSW)
        .build()

    private var meMarker: Symbol? = null
    private var map: MapboxMap? = null
    private val markerToPlayground: MutableMap<Symbol, Playground> = mutableMapOf()
    private val playgroundsOnMap = mutableSetOf<Playground>()
    private val loadedPlaygrounds = mutableListOf<Playground>()
    protected val codeAutocomplete = 1
    protected lateinit var autocompleteLocation: List<CarmenFeature>
    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val _myPermisionsRequestInt: Int = 199
    private var locationUpdatesAreAllowed: Boolean = false
    private var symbolManager: SymbolManager? = null
    private val _myPermissionRequestLocation: Int = 99

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        onCreate(savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var accessToken = this.applicationContext.fetchMapboxAccessToken
        if (accessToken.isEmpty()) {
            accessToken = getString(R.string.access_token)
        }
        Mapbox.getInstance(this, accessToken)

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

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val disabledGPS = sharedPreferences.getBoolean(getString(R.string.disabled_gps), false)

        if (!disabledGPS) {
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
                                _myPermisionsRequestInt
                            )
                        }
                        .create()
                        .show()


                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        _myPermisionsRequestInt
                    )
                }
            }
        }


        var mapboxUrl = this.applicationContext.fetchMapboxUrl
        if (mapboxUrl.isEmpty()) {

            mapboxUrl = getString(R.string.MAPBOX_STYLE)
        }
        Mapbox.getInstance(this, accessToken)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap
            mapboxMap.setStyle(mapboxUrl)

            mapboxMap.setStyle(mapboxUrl) { style ->
                style.addImage(
                    _playgroundMarkerIcon,
                    getBitmapFromVectorDrawable(this, R.mipmap.ic_launcher_round),
                    false
                )
                val activeUser = this.applicationContext.playgroundApp.activeUser
                var avatarURL = AvatarViewDialog.getDefaultAvatarURL()
                if (activeUser?.avatarURL !== null) {
                    avatarURL = activeUser.avatarURL
                }
                Glide.with(this /* context */)
                    .asBitmap()
                    .load(avatarURL)
                    .into<BitmapTarget>(object : BitmapTarget() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                            style.addImage(
                                _meMarkerIcon,
                                resource,
                                false
                            )
                        }
                    })

                symbolManager = SymbolManager(mapView, mapboxMap, style)
                symbolManager?.iconAllowOverlap = true
                symbolManager?.textAllowOverlap = true
                symbolManager?.addClickListener {
                    val playground = this.markerToPlayground[it]

                    when {
                        playground !== null -> {
                            PlaygroundListDialogFragment.newInstance(playground).show(supportFragmentManager, "dialog")
                        }
                        meMarker?.id == it.id -> {
                            try {
                                loadedPlaygrounds.filter { playground1 ->
                                    val playgroundLocation = Location("point B")
                                    playgroundLocation.latitude = playground1.location.latitude()
                                    playgroundLocation.longitude = playground1.location.longitude()

                                    val playgroundLocationA = Location("point A")

                                    playgroundLocationA.latitude = meMarker?.latLng?.latitude ?: 0.0
                                    playgroundLocationA.longitude = meMarker?.latLng?.longitude ?: 0.0
                                    playgroundLocationA.distanceTo(playgroundLocation) < 250
                                }.first { _playground ->
                                    PlaygroundListDialogFragment.newInstance(_playground)
                                        .show(supportFragmentManager, "dialog")
                                    true
                                }
                            } catch (e: Exception) {
                            }
                        }
                    }
                }

                val localizationPlugin = LocalizationPlugin(mapView!!, mapboxMap, style)
                localizationPlugin.setMapLanguage(MapLocale(MapLocale.GERMAN))

                try {
                    localizationPlugin.matchMapLanguageWithDeviceDefault()
                } catch (exception: RuntimeException) {

                }
                if (loadedPlaygrounds.isNotEmpty())
                    addMarkersToMap(mapboxMap, loadedPlaygrounds)
            }

            mapboxMap.setLatLngBoundsForCameraTarget(_restrictedBoundsArea)

            if (lastKnownLocation != null) {
                makeUseOfNewLocation(lastKnownLocation!!)
            }
        }

        initLocations()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager


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
                                _myPermissionRequestLocation
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
        val builder = LatLngBounds.Builder()

        val filteredPlaygrounds = playgrounds.filter { it !in playgroundsOnMap }
        filteredPlaygrounds.forEach {

            val latLng = LatLng(
                it.location.latitude(),
                it.location.longitude()
            )
            val symbolOptions = SymbolOptions()
                .withLatLng(latLng)
                .withIconImage(_playgroundMarkerIcon)
                .withIconSize(1.3f)

            val symbol = symbolManager?.create(symbolOptions)

            if (symbol !== null) {
                if (this.markerToPlayground.values.contains(it)) {
                    this.markerToPlayground.remove(symbol)
                }
                this.markerToPlayground[symbol] = it
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

        markerToPlayground.keys.subtract(activeMarkerToPlayground.keys).forEach {
            it.iconOpacity = 0.0f
            symbolManager?.update(it)
        }

        activeMarkerToPlayground.keys.forEach {
            it.iconOpacity = 1.0f
            symbolManager?.update(it)
        }
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

        if (resultCode == Activity.RESULT_OK && requestCode == codeAutocomplete) {

            // Retrieve selected location's CarmenFeature
            val selectedCarmenFeature = PlaceAutocomplete.getPlace(data)

            map?.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(
                            LatLng(
                                (selectedCarmenFeature.geometry() as Point).latitude(),
                                (selectedCarmenFeature.geometry() as Point).longitude()
                            )
                        )
                        .zoom(14.0)
                        .build()
                ), 4000
            )
        }
        if (requestCode == _myPermissionRequestLocation) {
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

    private fun makeUseOfNewLocation(location: Location) {

        val activeUser = this.applicationContext.playgroundApp.activeUser

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
                this
            )
        }
        moveMeMarker(location)
    }



    private fun moveMeMarker(location: Location) {

        val latLng = LatLng(location.latitude, location.longitude)
        if (meMarker === null) {

            val symbolOptions = SymbolOptions()
                .withLatLng(latLng)
                .withIconImage(_meMarkerIcon)
                .withIconSize(1f)

            symbolManager?.create(symbolOptions)
        } else {
            meMarker?.latLng = latLng
            symbolManager?.update(meMarker)
        }

        val builder = LatLngBounds.Builder()
        this@BaseMapboxActivity.playgroundsOnMap.forEach {
            builder.include(
                LatLng(
                    it.location.latitude(),
                    it.location.longitude()
                )
            )
        }

        if (this@BaseMapboxActivity.playgroundsOnMap.size > 1) {
            builder.include(latLng)
            map?.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150))
        } else {

            val newCameraPosition = CameraPosition.Builder()
                .target(latLng)
                .zoom(14.0)
                .build()
            map?.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition), 4000)
        }
    }

    private fun showNotificationAlert(count: Int, activity: FragmentActivity) {

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
                AlertDialog.Builder(this)
                    .setTitle(R.string.title_location_permission)
                    .setMessage(R.string.text_location_permission)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        ActivityCompat.requestPermissions(
                            this@BaseMapboxActivity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            _myPermisionsRequestInt
                        )
                    }
                    .create()
                    .show()


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    _myPermisionsRequestInt
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

        if (requestCode == _myPermisionsRequestInt) {
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