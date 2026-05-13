package com.suryashakti.solarmonitor.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import com.suryashakti.solarmonitor.R
import com.suryashakti.solarmonitor.data.AuthState
import com.suryashakti.solarmonitor.data.PanelLocationManager
import com.suryashakti.solarmonitor.databinding.ActivityAuthBinding
import com.suryashakti.solarmonitor.ui.location.PanelLocationActivity
import com.suryashakti.solarmonitor.ui.main.MainActivity
import com.suryashakti.solarmonitor.util.ThemeManager
import com.suryashakti.solarmonitor.viewmodel.AuthViewModel
import com.suryashakti.solarmonitor.viewmodel.AuthViewModelFactory

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    lateinit var authViewModel: AuthViewModel
        private set

    /** Whether we arrived here just to show welcome + location (already logged in) */
    private val showLocationDirect: Boolean
        get() = intent.getBooleanExtra(EXTRA_SHOW_LOCATION, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyWindow(window, this)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyToViewTree(binding.root)
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager,
                    fragment: Fragment,
                    view: View,
                    savedInstanceState: Bundle?
                ) {
                    ThemeManager.applyToViewTree(view)
                }
            },
            true
        )
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.auth_nav_host) as NavHostFragment
        navHostFragment.navController.addOnDestinationChangedListener { _, _, _ ->
            binding.root.post { applyCurrentTheme(navHostFragment) }
        }
        binding.root.post { applyCurrentTheme(navHostFragment) }

        authViewModel = ViewModelProvider(
            this, AuthViewModelFactory(application)
        )[AuthViewModel::class.java]

        // If already logged in and just need to set location
        if (showLocationDirect) {
            goToLocationPicker()
            return
        }

        authViewModel.authState.observe(this) { state ->
            if (state is AuthState.LoggedIn) {
                // Show welcome popup then navigate
                showWelcomePopup(state.user.shortName) {
                    if (!PanelLocationManager.isLocationSet(this)) {
                        goToLocationPicker()
                    } else {
                        navigateToMain()
                    }
                }
            }
        }

        startBackgroundAnimation()
    }

    private fun showWelcomePopup(name: String, onDone: () -> Unit) {
        // Full-screen welcome overlay card
        val overlay = FrameLayout(this)
        overlay.setBackgroundColor(0xCC000000.toInt())
        overlay.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        val card = layoutInflater.inflate(R.layout.view_welcome_popup, null)
        card.findViewById<TextView>(R.id.tvWelcomeUser).text =
            "Welcome,\n$name! ☀️"

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER }
        overlay.addView(card, lp)

        binding.root.addView(overlay)

        // Animate card in
        card.scaleX = 0.6f; card.scaleY = 0.6f; card.alpha = 0f
        card.animate().scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(500).setInterpolator(OvershootInterpolator(1.8f)).start()

        // Dismiss after 1.8 s
        binding.root.postDelayed({
            card.animate().scaleX(0.8f).scaleY(0.8f).alpha(0f)
                .setDuration(300).setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    binding.root.removeView(overlay)
                    onDone()
                }.start()
        }, 1800)
    }

    private fun goToLocationPicker() {
        startActivity(Intent(this, PanelLocationActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun startBackgroundAnimation() {
        binding.viewAuthGlow.animate()
            .scaleX(1.3f).scaleY(1.3f).setDuration(3000)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.viewAuthGlow.animate()
                    .scaleX(1f).scaleY(1f).setDuration(3000)
                    .withEndAction { startBackgroundAnimation() }.start()
            }.start()
    }

    private fun applyCurrentTheme(navHostFragment: NavHostFragment) {
        ThemeManager.applyWindow(window, this)
        ThemeManager.applyToViewTree(binding.root)
        navHostFragment.childFragmentManager.fragments.firstOrNull()?.view?.let {
            ThemeManager.applyToViewTree(it)
        }
    }

    companion object {
        const val EXTRA_SHOW_LOCATION = "show_location"
    }
}
