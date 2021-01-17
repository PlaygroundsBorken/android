package de.borken.playgrounds.borkenplaygrounds

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.drawable.PictureDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.PersistableBundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
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
import de.borken.playgrounds.borkenplaygrounds.databinding.ActivityPlaygroundBinding
import de.borken.playgrounds.borkenplaygrounds.fragments.AvatarViewDialog
import de.borken.playgrounds.borkenplaygrounds.fragments.PlaygroundListDialogFragment
import de.borken.playgrounds.borkenplaygrounds.glide.GlideRequest
import de.borken.playgrounds.borkenplaygrounds.models.Playground
import de.borken.playgrounds.borkenplaygrounds.models.PlaygroundElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

open class BaseMapboxActivity : AppCompatActivity(), LocationListener, CoroutineScope {
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private lateinit var requestBuilder: GlideRequest<PictureDrawable>
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
    private val loadedPlaygrounds = mutableListOf<Playground>()
    protected val codeAutocomplete = 1
    lateinit var autocompleteLocation: List<CarmenFeature>
    private lateinit var locationManager: LocationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val _myPermisionsRequestInt: Int = 199
    private var locationUpdatesAreAllowed: Boolean = false
    private var symbolManager: SymbolManager? = null
    private val _myPermissionRequestLocation: Int = 99
    protected lateinit var binding: ActivityPlaygroundBinding

    fun autoCompleteIsInitialized(): Boolean {

        return this::autocompleteLocation.isInitialized
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        onCreate(savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageLoader = ImageLoader.Builder(this)
            .componentRegistry {
                add(SvgDecoder(this@BaseMapboxActivity))
            }
            .build()

        var accessToken = this.applicationContext.fetchMapboxAccessToken
        if (accessToken.isEmpty()) {
            accessToken = getString(R.string.access_token)
        }
        Mapbox.getInstance(this, accessToken)

        binding = ActivityPlaygroundBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

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
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { mapboxMap ->
            map = mapboxMap

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


                val request = ImageRequest.Builder(this)
                    .data(avatarURL)
                    .allowHardware(false) // Disable hardware bitmaps.
                    .build()

                launch {
                    val drawable = (imageLoader.execute(request) as SuccessResult).drawable
                    style.removeImage(_meMarkerIcon)
                    style.addImage(
                        _meMarkerIcon,
                        drawable.toBitmap(260, 260)
                    )
                }


                symbolManager = SymbolManager(binding.mapView, mapboxMap, style)
                symbolManager?.iconAllowOverlap = true
                symbolManager?.textAllowOverlap = true
                symbolManager?.addClickListener {
                    val playgroundMap = this.markerToPlayground.filter {(symbol, _) ->
                        symbol.id == it.id
                    }
                    var playground: Playground? = null
                    if (playgroundMap.isNotEmpty()) {
                        playground = playgroundMap.values.first()
                    }

                    when {
                        playground !== null -> {
                            PlaygroundListDialogFragment.newInstance(playground).show(supportFragmentManager, "dialog")
                            true
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
                            true
                        }
                        else -> true
                    }
                }

                val localizationPlugin = LocalizationPlugin(binding.mapView, mapboxMap, style)
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
                    startLocationUpdates()
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

        playgrounds.filter { playground ->
            val filterValues = this.markerToPlayground.filterValues { it == playground }
            filterValues.isEmpty()
        }.forEach {

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
                this.markerToPlayground[symbol] = it
            }
        }

        playgrounds.forEach {
            val latLng = LatLng(
                it.location.latitude(),
                it.location.longitude()
            )

            builder.include(latLng)
        }

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
        }

        activeMarkerToPlayground.keys.forEach {
            it.iconOpacity = 1.0f
        }
        symbolManager?.update(markerToPlayground.keys.toList())
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

            meMarker = symbolManager?.create(symbolOptions)
        } else {
            meMarker?.latLng = latLng
            symbolManager?.update(meMarker)
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
                        R.style.AlertButton
                    ) {
                        AvatarViewDialog.newInstance().show(supportFragmentManager, "dialog")
                    }
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
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1.0f
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
        binding.mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        binding.mapView.onResume()

        startLocationUpdates()
    }

    public override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        stopLocationUpdates()
    }

    public override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()

        locationManager.removeUpdates(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onLocationChanged(location: Location) {
        makeUseOfNewLocation(location)
    }

    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
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