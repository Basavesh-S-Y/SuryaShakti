package com.suryashakti.solarmonitor.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.suryashakti.solarmonitor.R
import com.suryashakti.solarmonitor.databinding.ActivityMainBinding
import com.suryashakti.solarmonitor.ui.auth.AuthActivity
import com.suryashakti.solarmonitor.util.AuthManager
import com.suryashakti.solarmonitor.util.ThemeManager
import com.suryashakti.solarmonitor.viewmodel.ForecastViewModel
import com.suryashakti.solarmonitor.viewmodel.ForecastViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    lateinit var forecastViewModel: ForecastViewModel
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyWindow(window, this)

        // Guard: must be logged in
        if (!AuthManager.isLoggedIn) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish(); return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyToViewTree(binding.root)
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager,
                    fragment: Fragment,
                    view: android.view.View,
                    savedInstanceState: Bundle?
                ) {
                    ThemeManager.applyToViewTree(view)
                }
            },
            true
        )

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHostFragment.navController)
        navHostFragment.navController.addOnDestinationChangedListener { _, _, _ ->
            binding.root.post { applyCurrentTheme(navHostFragment) }
        }
        binding.root.post { applyCurrentTheme(navHostFragment) }

        // ForecastViewModel uses panel location (not GPS) — no permission needed
        forecastViewModel = ViewModelProvider(
            this, ForecastViewModelFactory(application)
        )[ForecastViewModel::class.java]
    }

    private fun applyCurrentTheme(navHostFragment: NavHostFragment) {
        ThemeManager.applyWindow(window, this)
        ThemeManager.applyToViewTree(binding.root)
        navHostFragment.childFragmentManager.fragments.firstOrNull()?.view?.let {
            ThemeManager.applyToViewTree(it)
        }
    }
}
