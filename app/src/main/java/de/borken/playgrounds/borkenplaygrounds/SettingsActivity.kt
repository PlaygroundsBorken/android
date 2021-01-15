package de.borken.playgrounds.borkenplaygrounds

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import de.borken.playgrounds.borkenplaygrounds.fragments.AvatarViewDialog


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupActionBar()

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, MySettingsFragment())
            .commit()
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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

    class MySettingsFragment : PreferenceFragmentCompat() {

        override fun onPreferenceTreeClick(preference: androidx.preference.Preference?): Boolean {

            activity?.getPreferences(Context.MODE_PRIVATE)
            when (preference?.key) {
                "sponsor" -> {

                    val viewIntent = Intent(activity, SponsorActivity::class.java)
                    startActivity(viewIntent)
                    return true
                }
                "select_location_settings" -> {
                    val viewIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(viewIntent)
                    return true
                }
                "avatarSettingsId" -> {
                    AvatarViewDialog.newInstance().show(parentFragmentManager, "dialog")
                    return true
                }
                "intro" -> {

                    val intent =
                        Intent(activity, IntroActivity::class.java) // Call the AppIntro java class
                    startActivity(intent)
                    return true
                }
                "remark_link" -> {
                    val url =
                        "https://www.borken.de/buergerservice/ideen-und-beschwerdemanagement/ideen-u-beschwerdemanagement.html"
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse(url)
                    startActivity(i)

                    return true
                }
                "software_licenses" -> {
                    val intent = Intent(activity, OssLicensesMenuActivity::class.java)
                    startActivity(intent)
                    return true
                }
                "impressum" -> {
                    val intent = Intent(activity, SimpleWebView::class.java)
                    intent.putExtra("key", "impressum")
                    startActivity(intent)
                    return true
                }
                "privacy_policy" -> {
                    val intent = Intent(activity, SimpleWebView::class.java)
                    intent.putExtra("key", "privacy_policy")
                    startActivity(intent)
                    return true
                }
            }
            return super.onPreferenceTreeClick(preference)
        }

        override fun onCreatePreferences(p0: Bundle?, p1: String?) {
            setPreferencesFromResource(R.xml.pref_headers, null)
        }
    }
}
