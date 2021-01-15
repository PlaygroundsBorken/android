package de.borken.playgrounds.borkenplaygrounds

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebSettings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import de.borken.playgrounds.borkenplaygrounds.databinding.ActivitySimpleWebViewBinding


class SimpleWebView : AppCompatActivity() {

    private lateinit var binding: ActivitySimpleWebViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleWebViewBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setupActionBar()

        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.settings.useWideViewPort = true
        val bundle = intent.extras
        val key = bundle!!.getString("key")
        if (key != null) {
            val content = application.playgroundApp.fetchByString(key)
            binding.webView.loadData(content, "text/html; charset=utf-8", "utf-8")
        }

        val webSettings = binding.webView.settings
        val res = resources
        val fontSize = res.getDimension(R.dimen.txtSize)
        webSettings.defaultFontSize = fontSize.toInt()
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.blockNetworkImage = true
        webSettings.loadsImagesAutomatically = true
        webSettings.setGeolocationEnabled(false)
        webSettings.setNeedInitialFocus(false)
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
