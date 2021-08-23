package com.jclemus.playinstallreferrer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.jclemus.playinstallreferrer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var referrerClient: InstallReferrerClient
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        evaluateReferrer()
    }

    private fun evaluateReferrer() {
        referrerClient = InstallReferrerClient.newBuilder(this).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        // Connection established.
                        handleReferrer()
                        referrerClient.endConnection()
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        // API not available on the current Play Store app.
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        // Connection couldn't be established.
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun handleReferrer() {
        val response: ReferrerDetails = referrerClient.installReferrer
        val referrerUrl: String = response.installReferrer
        val referrerClickTime: Long = response.referrerClickTimestampSeconds
        val appInstallTime: Long = response.installBeginTimestampSeconds
        val instantExperienceLaunched: Boolean = response.googlePlayInstantParam

        val stringBuilder = StringBuilder()
        stringBuilder.append("referrerUrl: $referrerUrl")
        stringBuilder.append("referrerClickTime: $referrerClickTime")
        stringBuilder.append("appInstallTime: $appInstallTime")
        stringBuilder.append("instantExperienceLaunched: $instantExperienceLaunched")
        showDialog(referrerUrl)

        preselectTab(referrerUrl)
    }

    private fun showDialog(description: String) {
        AlertDialog.Builder(this)
            .setTitle("Referrer Result")
            .setMessage(description)
            .setPositiveButton("Close", null)
            .create()
            .show()

    }

    private fun preselectTab(referredUrl: String) {
        val items = referredUrl.split("&")
        val rawReferrer: String? = items.firstOrNull {
            it.contains("referrer")
        }

        if (rawReferrer?.isNotBlank() == true) {
            val referredValue = rawReferrer.split("=")
            if (referredValue.size >= 2) {
                val id = when (referredValue[1].toInt()) {
                    0 -> R.id.navigation_home
                    1 -> R.id.navigation_dashboard
                    else -> R.id.navigation_notifications
                }
                binding.navView.selectedItemId = id
                Toast.makeText(this, "Referrer - selected position: ${referredValue[1]}", Toast.LENGTH_LONG).show()
            }
        }
    }

}