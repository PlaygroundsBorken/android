package de.borken.playgrounds.borkenplaygrounds

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_sponsor.*

class SponsorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sponsor)
        setupActionBar()

        Glide.with(this).load(R.drawable.logo_jugendwerk).into(jugendwerk)
        Glide.with(this).load(R.drawable.logo_borken).into(borken)
        Glide.with(this).load(R.drawable.logo_lwl).into(lwl)
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
