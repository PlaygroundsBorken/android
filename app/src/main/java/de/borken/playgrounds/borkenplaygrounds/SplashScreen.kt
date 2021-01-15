package de.borken.playgrounds.borkenplaygrounds

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import de.borken.playgrounds.borkenplaygrounds.databinding.ActivitySplashScreenBinding
import de.borken.playgrounds.borkenplaygrounds.models.User

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SplashScreen : AppCompatActivity(), User.UserCreated {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivitySplashScreenBinding

    override fun userIsCreated(user: User?) {
        applicationContext.playgroundApp.activeUser = user
        binding.progressBar.progress = 90
        finalizeSplashScreen()
    }

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.progressBar.progress = 0

        Glide.with(this /* context */)
            .load("https://res.cloudinary.com/tbuning/image/upload/c_scale,h_800/v1543067149/badges/Logo-Turmhaus.webp")
            .into(binding.imageView)
        binding.progressBar.progress = 4
        val deviceId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )

        binding.rules.text = application.fetchRules

        auth = FirebaseAuth.getInstance()

        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build()
                    val db = FirebaseFirestore.getInstance()
                    db.firestoreSettings = settings
                    binding.progressBar.progress = 6
                    db.collection("users")
                        .whereEqualTo("deviceId", deviceId)
                        .get()
                        .addOnSuccessListener {
                            binding.progressBar.progress = 30
                            val user = User.tryParse(it)
                            if (user == null) {
                                binding.progressBar.progress = 50
                                User.createNewUser(deviceId, this)
                            } else {
                                binding.progressBar.progress = 90
                                applicationContext.playgroundApp.activeUser = user
                            }
                            requestPermission()
                        }.addOnFailureListener {

                            finalizeSplashScreen()
                        }
                } else {
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private val permissionRequestCoarseLocation: Int = 10002

    private fun requestPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_NETWORK_STATE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_NETWORK_STATE
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_NETWORK_STATE),
                    permissionRequestCoarseLocation
                )

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            finalizeSplashScreen()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        finalizeSplashScreen()
    }

    private fun finalizeSplashScreen() {
        binding.progressBar.progress = 100

        val intent = Intent(
            applicationContext,
            PlaygroundActivity::class.java
        )
        startActivity(intent)
        finish()
    }
}
