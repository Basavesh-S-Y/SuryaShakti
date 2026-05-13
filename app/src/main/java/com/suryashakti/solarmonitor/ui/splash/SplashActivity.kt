package com.suryashakti.solarmonitor.ui.splash

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.suryashakti.solarmonitor.databinding.ActivitySplashBinding
import com.suryashakti.solarmonitor.data.PanelLocationManager
import com.suryashakti.solarmonitor.ui.auth.AuthActivity
import com.suryashakti.solarmonitor.ui.main.MainActivity
import com.suryashakti.solarmonitor.util.AuthManager
import com.suryashakti.solarmonitor.util.ThemeManager

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyWindow(window, this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyToViewTree(binding.root)

        binding.ivSun.alpha = 0f
        binding.ivSun.scaleX = 0.3f; binding.ivSun.scaleY = 0.3f
        binding.tvAppName.alpha = 0f; binding.tvAppName.translationY = 60f
        binding.tvSubtitle.alpha = 0f; binding.progressBar.alpha = 0f

        binding.ivSun.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(900).setInterpolator(OvershootInterpolator(1.2f)).start()

        ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 8000; repeatCount = ValueAnimator.INFINITE
            addUpdateListener { binding.ivSunRays.rotation = it.animatedValue as Float }
            start()
        }
        binding.tvAppName.animate().alpha(1f).translationY(0f)
            .setDuration(700).setStartDelay(600).setInterpolator(DecelerateInterpolator()).start()
        binding.tvSubtitle.animate().alpha(1f)
            .setDuration(600).setStartDelay(900).start()
        binding.progressBar.animate().alpha(1f)
            .setDuration(400).setStartDelay(1200).start()

        binding.root.postDelayed({ navigateNext() }, 2800)
    }

    private fun navigateNext() {
        val destination = when {
            // 1. Not logged in → go to Auth (Auth handles location after login)
            !AuthManager.isLoggedIn ->
                Intent(this, AuthActivity::class.java)
            // 2. Logged in but location not set → go to Auth which will show welcome + location
            !PanelLocationManager.isLocationSet(this) ->
                Intent(this, AuthActivity::class.java)
                    .putExtra(AuthActivity.EXTRA_SHOW_LOCATION, true)
            // 3. All good → Main
            else ->
                Intent(this, MainActivity::class.java)
        }
        startActivity(destination)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
