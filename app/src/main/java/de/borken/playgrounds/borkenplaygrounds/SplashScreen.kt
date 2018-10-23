package de.borken.playgrounds.borkenplaygrounds

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_splash_screen.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash_screen)

        // Reference to an image file in Cloud Storage
        val storageReference = FirebaseStorage.getInstance().reference.child("badges/slide.png")

        Glide.with(this /* context */)
            .load(storageReference)
            .into(imageView)

        val intent = Intent(
            applicationContext,
            PlaygroundActivity::class.java
        )
        startActivity(intent)
        finish()
    }
}
