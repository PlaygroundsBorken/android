package de.borken.playgrounds.borkenplaygrounds

import android.os.Bundle
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.webkit.WebSettings
import kotlinx.android.synthetic.main.activity_simple_web_view.*


class SimpleWebView : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_web_view)
        setupActionBar()

        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        val bundle = intent.extras
        val key = bundle!!.getString("key")
        if (key != null) {
            val content = application.playgroundApp.fetchByString(key)
            webView.loadData(content, "text/html; charset=utf-8", "utf-8")
        }

        val webSettings = webView.settings
        val res = resources
        val fontSize = res.getDimension(R.dimen.txtSize)
        webSettings.defaultFontSize = fontSize.toInt()
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.setAppCacheEnabled(false)
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
