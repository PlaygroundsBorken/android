package de.borken.playgrounds.borkenplaygrounds

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import de.borken.playgrounds.borkenplaygrounds.models.User
import kotlinx.android.synthetic.main.activity_splash_screen.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SplashScreen : AppCompatActivity(), User.UserCreated {

    override fun userIsCreated(user: User?) {
        applicationContext.playgroundApp.activeUser = user
        progressBar.progress = 90
        finalizeSplashScreen()
    }

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash_screen)

        progressBar.progress = 0

        Glide.with(this /* context */)
            .load("https://res.cloudinary.com/tbuning/image/upload/c_scale,h_800/v1543067149/badges/Logo-Turmhaus.webp")
            .into(imageView)
        progressBar.progress = 4
        val deviceId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = settings
        progressBar.progress = 6
        db.collection("users")
            .whereEqualTo("deviceId", deviceId)
            .get()
            .addOnSuccessListener {
                progressBar.progress = 30
                val user = User.tryParse(it)
                if (user == null) {
                    progressBar.progress = 50
                    User.createNewUser(deviceId, this)
                } else {
                    progressBar.progress = 90
                    applicationContext.playgroundApp.activeUser = user
                    finalizeSplashScreen()
                }
            }.addOnFailureListener {

                finalizeSplashScreen()
            }
    }

    private fun finalizeSplashScreen() {
        progressBar.progress = 100
        val intent = Intent(
            applicationContext,
            PlaygroundActivity::class.java
        )
        startActivity(intent)
        finish()
    }
}
