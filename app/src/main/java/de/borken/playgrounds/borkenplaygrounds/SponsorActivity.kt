package de.borken.playgrounds.borkenplaygrounds

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.bumptech.glide.Glide
import de.borken.playgrounds.borkenplaygrounds.databinding.ActivitySponsorBinding

class SponsorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySponsorBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setupActionBar()

        Glide.with(this).load(R.drawable.logo_jugendwerk).into(binding.jugendwerk)
        Glide.with(this).load(R.drawable.logo_borken).into(binding.borken)
        Glide.with(this).load(R.drawable.logo_lwl).into(binding.lwl)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
